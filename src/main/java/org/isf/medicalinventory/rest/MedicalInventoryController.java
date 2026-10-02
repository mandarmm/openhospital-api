/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2026 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.isf.medicalinventory.rest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.isf.medical.mapper.MedicalMapper;
import org.isf.medicalinventory.dto.InventoryWithRowsDTO;
import org.isf.medicalinventory.dto.MedicalInventoryDTO;
import org.isf.medicalinventory.dto.MedicalInventoryRowDTO;
import org.isf.medicalinventory.manager.MedicalInventoryManager;
import org.isf.medicalinventory.manager.MedicalInventoryRowManager;
import org.isf.medicalinventory.model.InventoryStatus;
import org.isf.medicalinventory.model.InventoryType;
import org.isf.medicalinventory.model.MedicalInventory;
import org.isf.medicalinventory.model.MedicalInventoryRow;
import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.medicalstock.manager.MovStockInsertingManager;
import org.isf.medicalstock.mapper.LotMapper;
import org.isf.medicalstock.model.Lot;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.utils.exception.OHDataValidationException;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Inventories of the main store and of the wards (core {@code MedicalInventoryManager}): a draft with the counted
 * quantities, validated (brought up to date with the movements saved since the count), then confirmed, which books
 * the differences as movements.
 * <p>
 * OH has no inventory permission: a main store inventory needs the main store permissions
 * ({@code medicalstockmovements.*}), a ward inventory the ward stock ones ({@code medicalstockward.*}).
 */
@RestController
@Tag(name = "Medical inventories")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(value = "/medicalinventories", produces = MediaType.APPLICATION_JSON_VALUE)
public class MedicalInventoryController {

	private final MedicalInventoryManager inventoryManager;

	private final MedicalInventoryRowManager rowManager;

	private final MedicalBrowsingManager medicalManager;

	private final MovStockInsertingManager movStockInsertingManager;

	private final MedicalMapper medicalMapper;

	private final LotMapper lotMapper;

	public MedicalInventoryController(MedicalInventoryManager inventoryManager, MedicalInventoryRowManager rowManager,
		MedicalBrowsingManager medicalManager, MovStockInsertingManager movStockInsertingManager, MedicalMapper medicalMapper,
		LotMapper lotMapper) {
		this.inventoryManager = inventoryManager;
		this.rowManager = rowManager;
		this.medicalManager = medicalManager;
		this.movStockInsertingManager = movStockInsertingManager;
		this.medicalMapper = medicalMapper;
		this.lotMapper = lotMapper;
	}

	/**
	 * The inventories of a type ({@code main} or {@code ward}), optionally of a ward and status, counted in the days.
	 */
	@GetMapping
	public List<MedicalInventoryDTO> getInventories(
		@RequestParam("type") String type,
		@RequestParam(name = "ward", required = false) String wardCode,
		@RequestParam(name = "status", required = false) String status,
		@RequestParam(name = "from", required = false) LocalDate from,
		@RequestParam(name = "to", required = false) LocalDate to
	) throws OHServiceException {
		checkPermission(type, "read");
		return inventoryManager.getMedicalInventory().stream()
			.filter(inventory -> type.equals(inventory.getInventoryType()))
			.filter(inventory -> wardCode == null || wardCode.equals(inventory.getWardCode()))
			.filter(inventory -> status == null || status.equals(inventory.getStatus()))
			.filter(inventory -> from == null || !inventory.getInventoryDate().toLocalDate().isBefore(from))
			.filter(inventory -> to == null || !inventory.getInventoryDate().toLocalDate().isAfter(to))
			.map(this::toDTO)
			.toList();
	}

	@GetMapping("/{id}")
	public InventoryWithRowsDTO getInventory(@PathVariable("id") int id) throws OHServiceException {
		MedicalInventory inventory = find(id);
		checkPermission(inventory.getInventoryType(), "read");
		return withRows(inventory);
	}

	/**
	 * Creates a draft inventory with its rows.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional(rollbackFor = OHServiceException.class)
	public InventoryWithRowsDTO newInventory(@RequestBody InventoryWithRowsDTO body) throws OHServiceException {
		MedicalInventoryDTO dto = body.getInventory();
		if (dto == null || !isType(dto.getInventoryType())) {
			throw new OHAPIException(new OHExceptionMessage("The inventory type must be main or ward."));
		}
		checkPermission(dto.getInventoryType(), "create");
		MedicalInventory inventory = new MedicalInventory(null, InventoryStatus.draft.toString(), dto.getInventoryDate(), currentUser(),
			dto.getInventoryReference(), dto.getInventoryType(), dto.getWardCode());
		copyParameters(dto, inventory);
		MedicalInventory saved = inventoryManager.newMedicalInventory(inventory, List.of());
		for (MedicalInventoryRowDTO row : body.getRows()) {
			rowManager.newMedicalInventoryRow(toModel(row, saved));
		}
		return withRows(saved);
	}

	/**
	 * Updates a draft or validated inventory and its rows (rows left out are deleted); it becomes a draft again.
	 */
	@PutMapping("/{id}")
	@Transactional(rollbackFor = OHServiceException.class)
	public InventoryWithRowsDTO updateInventory(@PathVariable("id") int id, @RequestBody InventoryWithRowsDTO body) throws OHServiceException {
		MedicalInventory inventory = findEditable(id);
		checkPermission(inventory.getInventoryType(), "update");
		MedicalInventoryDTO dto = body.getInventory();
		if (dto != null) {
			inventory.setInventoryReference(dto.getInventoryReference());
			inventory.setInventoryDate(dto.getInventoryDate());
			copyParameters(dto, inventory);
		}
		inventory.setStatus(InventoryStatus.draft.toString());
		MedicalInventory saved = inventoryManager.updateMedicalInventory(inventory, true);

		List<MedicalInventoryRow> stored = rowManager.getMedicalInventoryRowByInventoryId(id);
		Map<Integer, MedicalInventoryRow> storedById = stored.stream()
			.collect(Collectors.toMap(MedicalInventoryRow::getId, Function.identity()));
		Set<Integer> kept = body.getRows().stream().map(MedicalInventoryRowDTO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		List<MedicalInventoryRow> removed = stored.stream().filter(row -> !kept.contains(row.getId())).toList();
		if (!removed.isEmpty()) {
			rowManager.deleteMedicalInventoryRows(new ArrayList<>(removed));
		}
		for (MedicalInventoryRowDTO row : body.getRows()) {
			MedicalInventoryRow existing = row.getId() == null ? null : storedById.get(row.getId());
			if (existing == null) {
				rowManager.newMedicalInventoryRow(toModel(row, saved));
			} else {
				existing.setTheoreticQty(row.getTheoreticQty());
				existing.setRealQty(row.getRealQty());
				rowManager.updateMedicalInventoryRow(existing);
			}
		}
		return withRows(saved);
	}

	/**
	 * Validates the inventory. When movements saved since the count changed the stock, answers 409 with what changed,
	 * unless {@code actualize}: then the theoretical quantities are brought up to date and the inventory is validated.
	 *
	 * @param allMedicals check the movements of all the medicals, not only those of the rows
	 */
	@PostMapping("/{id}/validate")
	@Transactional(rollbackFor = OHServiceException.class)
	public ResponseEntity<Object> validateInventory(
		@PathVariable("id") int id,
		@RequestParam(name = "allMedicals", defaultValue = "false") boolean allMedicals,
		@RequestParam(name = "actualize", defaultValue = "false") boolean actualize
	) throws OHServiceException {
		MedicalInventory inventory = findEditable(id);
		boolean ward = InventoryType.ward.toString().equals(inventory.getInventoryType());
		checkPermission(inventory.getInventoryType(), "update");
		List<MedicalInventoryRow> rows = rowManager.getMedicalInventoryRowByInventoryId(id);
		checkReadyForValidation(inventory, rows, ward);
		try {
			if (ward) {
				inventoryManager.validateMedicalWardInventoryRow(inventory, rows, allMedicals);
			} else {
				inventoryManager.validateMedicalInventoryRow(inventory, rows, allMedicals);
			}
			inventory.setStatus(InventoryStatus.validated.toString());
			inventoryManager.updateMedicalInventory(inventory, true);
		} catch (OHDataValidationException changes) {
			if (!actualize) {
				// the inventory stays as it was: its quantities are out of date
				return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(changes.getMessages().stream().map(OHExceptionMessage::getMessage).toList());
			}
			// as Swing: brought up to date (and saved) as validated
			inventory.setStatus(InventoryStatus.validated.toString());
			if (ward) {
				inventoryManager.actualizeMedicalWardInventoryRow(inventory, allMedicals);
			} else {
				inventoryManager.actualizeMedicalInventoryRow(inventory, allMedicals);
			}
		}
		return ResponseEntity.ok(withRows(find(id)));
	}

	/**
	 * Confirms a validated inventory: the differences are booked as movements and the inventory is done.
	 */
	@PostMapping("/{id}/confirm")
	@Transactional(rollbackFor = OHServiceException.class)
	public InventoryWithRowsDTO confirmInventory(
		@PathVariable("id") int id,
		@RequestParam(name = "allMedicals", defaultValue = "false") boolean allMedicals
	) throws OHServiceException {
		MedicalInventory inventory = find(id);
		checkPermission(inventory.getInventoryType(), "create");
		if (!InventoryStatus.validated.toString().equals(inventory.getStatus())) {
			throw new OHAPIException(new OHExceptionMessage("Only a validated inventory can be confirmed."));
		}
		List<MedicalInventoryRow> rows = rowManager.getMedicalInventoryRowByInventoryId(id);
		if (InventoryType.ward.toString().equals(inventory.getInventoryType())) {
			inventoryManager.confirmMedicalWardInventoryRow(inventory, rows, allMedicals);
		} else {
			inventoryManager.confirmMedicalInventoryRow(inventory, rows, allMedicals);
		}
		return withRows(find(id));
	}

	/**
	 * Cancels a draft or validated inventory: the core keeps it and its rows, marked canceled (lots of new rows are
	 * deleted).
	 */
	@DeleteMapping("/{id}")
	public boolean deleteInventory(@PathVariable("id") int id) throws OHServiceException {
		MedicalInventory inventory = findEditable(id);
		checkPermission(inventory.getInventoryType(), "delete");
		inventoryManager.deleteInventory(inventory);
		return true;
	}

	private void checkReadyForValidation(MedicalInventory inventory, List<MedicalInventoryRow> rows, boolean ward) throws OHAPIException {
		if (rows.isEmpty()) {
			throw new OHAPIException(new OHExceptionMessage("angal.inventory.cannotvalidateinventorywithoutproducts.msg"));
		}
		if (rows.stream().anyMatch(row -> row.getLot() == null)) {
			throw new OHAPIException(new OHExceptionMessage("angal.inventory.allinventoryrowshouldhavelotbeforevalidation.msg"));
		}
		if (ward) {
			return;
		}
		// as Swing checks before validating a main store inventory
		if (inventory.getChargeType() == null) {
			throw new OHAPIException(new OHExceptionMessage("angal.inventory.choosechargetypebeforevalidation.msg"));
		}
		if (inventory.getDischargeType() == null) {
			throw new OHAPIException(new OHExceptionMessage("angal.inventory.choosedischargetypebeforevalidation.msg"));
		}
		if (inventory.getSupplier() == null) {
			throw new OHAPIException(new OHExceptionMessage("angal.inventory.choosesupplierbeforevalidation.msg"));
		}
		if (inventory.getDestination() == null) {
			throw new OHAPIException(new OHExceptionMessage("angal.inventory.choosedestinationbeforevalidation.msg"));
		}
	}

	private MedicalInventory find(int id) throws OHServiceException {
		MedicalInventory inventory = inventoryManager.getInventoryById(id);
		if (inventory == null) {
			throw new OHAPIException(new OHExceptionMessage("Inventory not found."), HttpStatus.NOT_FOUND);
		}
		return inventory;
	}

	/** An inventory that can still change: a draft or a validated one. */
	private MedicalInventory findEditable(int id) throws OHServiceException {
		MedicalInventory inventory = find(id);
		String status = inventory.getStatus();
		if (!InventoryStatus.draft.toString().equals(status) && !InventoryStatus.validated.toString().equals(status)) {
			throw new OHAPIException(new OHExceptionMessage("This inventory can not be changed because its status is " + status + "."));
		}
		return inventory;
	}

	private static boolean isType(String type) {
		return InventoryType.main.toString().equals(type) || InventoryType.ward.toString().equals(type);
	}

	/** The stock permission for the inventory's type. */
	private static void checkPermission(String type, String action) throws OHAPIException {
		String resource = InventoryType.ward.toString().equals(type) ? "medicalstockward" : "medicalstockmovements";
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean granted = authentication != null && authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch((resource + "." + action)::equals);
		if (!granted) {
			throw new OHAPIException(new OHExceptionMessage("Forbidden: " + resource + "." + action + " is needed."), HttpStatus.FORBIDDEN);
		}
	}

	private static String currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? null : authentication.getName();
	}

	private static void copyParameters(MedicalInventoryDTO dto, MedicalInventory inventory) {
		inventory.setChargeType(dto.getChargeType());
		inventory.setDischargeType(dto.getDischargeType());
		inventory.setSupplier(dto.getSupplier());
		inventory.setDestination(dto.getDestination());
	}

	private MedicalInventoryRow toModel(MedicalInventoryRowDTO dto, MedicalInventory inventory) throws OHServiceException {
		if (dto.getMedical() == null || dto.getMedical().getCode() == null) {
			throw new OHAPIException(new OHExceptionMessage("angal.inventory.pleaseinsertmedical.msg"));
		}
		Medical medical = medicalManager.getMedical(dto.getMedical().getCode());
		Lot lot = dto.getLot() == null || dto.getLot().getCode() == null ? null : movStockInsertingManager.getLot(dto.getLot().getCode());
		if (dto.getLot() != null && dto.getLot().getCode() != null && lot == null) {
			throw new OHAPIException(new OHExceptionMessage("Lot not found: " + dto.getLot().getCode()));
		}
		return new MedicalInventoryRow(null, dto.getTheoreticQty(), dto.getRealQty(), inventory, medical, lot);
	}

	private MedicalInventoryDTO toDTO(MedicalInventory inventory) {
		MedicalInventoryDTO dto = new MedicalInventoryDTO();
		dto.setId(inventory.getId());
		dto.setStatus(inventory.getStatus());
		dto.setInventoryDate(inventory.getInventoryDate());
		dto.setUser(inventory.getUser());
		dto.setInventoryReference(inventory.getInventoryReference());
		dto.setInventoryType(inventory.getInventoryType());
		dto.setWardCode(inventory.getWardCode());
		dto.setChargeType(inventory.getChargeType());
		dto.setDischargeType(inventory.getDischargeType());
		dto.setSupplier(inventory.getSupplier());
		dto.setDestination(inventory.getDestination());
		dto.setLock(inventory.getLock());
		return dto;
	}

	private MedicalInventoryRowDTO toDTO(MedicalInventoryRow row) {
		MedicalInventoryRowDTO dto = new MedicalInventoryRowDTO();
		dto.setId(row.getId());
		dto.setTheoreticQty(row.getTheoreticQty());
		dto.setRealQty(row.getRealQty());
		dto.setMedical(row.getMedical() == null ? null : medicalMapper.map2DTO(row.getMedical()));
		dto.setLot(row.getLot() == null ? null : lotMapper.map2DTO(row.getLot()));
		dto.setNewLot(row.isNewLot());
		return dto;
	}

	private InventoryWithRowsDTO withRows(MedicalInventory inventory) throws OHServiceException {
		List<MedicalInventoryRowDTO> rows = rowManager.getMedicalInventoryRowByInventoryId(inventory.getId()).stream()
			.map(this::toDTO)
			.toList();
		return new InventoryWithRowsDTO(toDTO(inventory), rows);
	}
}

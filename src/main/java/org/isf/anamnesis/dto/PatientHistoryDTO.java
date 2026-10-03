/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2023 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.anamnesis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A patient's history (anamnesis): family history, past and recent problems, surgery, allergies, therapies, usual
 * medicines and the physiological history. One per patient.
 */
@Schema(description = "Patient history (anamnesis)")
public class PatientHistoryDTO {

	private int id;
	private int patientId;
	private boolean familyNothing;
	private boolean familyHypertension;
	private boolean familyDrugAddiction;
	private boolean familyCardiovascular;
	private boolean familyInfective;
	private boolean familyEndocrinometabol;
	private boolean familyRespiratory;
	private boolean familyCancer;
	private boolean familyOrto;
	private boolean familyGyno;
	private boolean familyOther;
	private String familyNote;
	private boolean patClosedNothing;
	private boolean patClosedHypertension;
	private boolean patClosedDrugaddiction;
	private boolean patClosedCardiovascular;
	private boolean patClosedInfective;
	private boolean patClosedEndocrinometabol;
	private boolean patClosedRespiratory;
	private boolean patClosedCancer;
	private boolean patClosedOrto;
	private boolean patClosedGyno;
	private boolean patClosedOther;
	private String patClosedNote;
	private boolean patOpenNothing;
	private boolean patOpenHypertension;
	private boolean patOpenDrugaddiction;
	private boolean patOpenCardiovascular;
	private boolean patOpenInfective;
	private boolean patOpenEndocrinometabol;
	private boolean patOpenRespiratory;
	private boolean patOpenCancer;
	private boolean patOpenOrto;
	private boolean patOpenGyno;
	private boolean patOpenOther;
	private String patOpenNote;
	private String patSurgery;
	private String patAllergy;
	private String patTherapy;
	private String patMedicine;
	private String patNote;
	private boolean phyNutritionNormal = true;
	private String phyNutritionAbnormal;
	private boolean phyBowelNormal = true;
	private String phyBowelAbnormal;
	private boolean phyDiuresisNormal = true;
	private String phyDiuresisAbnormal;
	private boolean phyAlcohol;
	private boolean phySmoke;
	private boolean phyDrug;
	private boolean phyPeriodNormal = true;
	private String phyPeriodAbnormal;
	private boolean phyMenopause;
	private int phyMenopauseYears;
	private boolean phyHrtNormal = true;
	private String phyHrtAbnormal;
	private boolean phyPregnancy;
	private int phyPregnancyNumber;
	private int phyPregnancyBirth;
	private int phyPregnancyAbort;
	private int lock;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPatientId() {
		return patientId;
	}

	public void setPatientId(int patientId) {
		this.patientId = patientId;
	}

	public boolean isFamilyNothing() {
		return familyNothing;
	}

	public void setFamilyNothing(boolean familyNothing) {
		this.familyNothing = familyNothing;
	}

	public boolean isFamilyHypertension() {
		return familyHypertension;
	}

	public void setFamilyHypertension(boolean familyHypertension) {
		this.familyHypertension = familyHypertension;
	}

	public boolean isFamilyDrugAddiction() {
		return familyDrugAddiction;
	}

	public void setFamilyDrugAddiction(boolean familyDrugAddiction) {
		this.familyDrugAddiction = familyDrugAddiction;
	}

	public boolean isFamilyCardiovascular() {
		return familyCardiovascular;
	}

	public void setFamilyCardiovascular(boolean familyCardiovascular) {
		this.familyCardiovascular = familyCardiovascular;
	}

	public boolean isFamilyInfective() {
		return familyInfective;
	}

	public void setFamilyInfective(boolean familyInfective) {
		this.familyInfective = familyInfective;
	}

	public boolean isFamilyEndocrinometabol() {
		return familyEndocrinometabol;
	}

	public void setFamilyEndocrinometabol(boolean familyEndocrinometabol) {
		this.familyEndocrinometabol = familyEndocrinometabol;
	}

	public boolean isFamilyRespiratory() {
		return familyRespiratory;
	}

	public void setFamilyRespiratory(boolean familyRespiratory) {
		this.familyRespiratory = familyRespiratory;
	}

	public boolean isFamilyCancer() {
		return familyCancer;
	}

	public void setFamilyCancer(boolean familyCancer) {
		this.familyCancer = familyCancer;
	}

	public boolean isFamilyOrto() {
		return familyOrto;
	}

	public void setFamilyOrto(boolean familyOrto) {
		this.familyOrto = familyOrto;
	}

	public boolean isFamilyGyno() {
		return familyGyno;
	}

	public void setFamilyGyno(boolean familyGyno) {
		this.familyGyno = familyGyno;
	}

	public boolean isFamilyOther() {
		return familyOther;
	}

	public void setFamilyOther(boolean familyOther) {
		this.familyOther = familyOther;
	}

	public String getFamilyNote() {
		return familyNote;
	}

	public void setFamilyNote(String familyNote) {
		this.familyNote = familyNote;
	}

	public boolean isPatClosedNothing() {
		return patClosedNothing;
	}

	public void setPatClosedNothing(boolean patClosedNothing) {
		this.patClosedNothing = patClosedNothing;
	}

	public boolean isPatClosedHypertension() {
		return patClosedHypertension;
	}

	public void setPatClosedHypertension(boolean patClosedHypertension) {
		this.patClosedHypertension = patClosedHypertension;
	}

	public boolean isPatClosedDrugaddiction() {
		return patClosedDrugaddiction;
	}

	public void setPatClosedDrugaddiction(boolean patClosedDrugaddiction) {
		this.patClosedDrugaddiction = patClosedDrugaddiction;
	}

	public boolean isPatClosedCardiovascular() {
		return patClosedCardiovascular;
	}

	public void setPatClosedCardiovascular(boolean patClosedCardiovascular) {
		this.patClosedCardiovascular = patClosedCardiovascular;
	}

	public boolean isPatClosedInfective() {
		return patClosedInfective;
	}

	public void setPatClosedInfective(boolean patClosedInfective) {
		this.patClosedInfective = patClosedInfective;
	}

	public boolean isPatClosedEndocrinometabol() {
		return patClosedEndocrinometabol;
	}

	public void setPatClosedEndocrinometabol(boolean patClosedEndocrinometabol) {
		this.patClosedEndocrinometabol = patClosedEndocrinometabol;
	}

	public boolean isPatClosedRespiratory() {
		return patClosedRespiratory;
	}

	public void setPatClosedRespiratory(boolean patClosedRespiratory) {
		this.patClosedRespiratory = patClosedRespiratory;
	}

	public boolean isPatClosedCancer() {
		return patClosedCancer;
	}

	public void setPatClosedCancer(boolean patClosedCancer) {
		this.patClosedCancer = patClosedCancer;
	}

	public boolean isPatClosedOrto() {
		return patClosedOrto;
	}

	public void setPatClosedOrto(boolean patClosedOrto) {
		this.patClosedOrto = patClosedOrto;
	}

	public boolean isPatClosedGyno() {
		return patClosedGyno;
	}

	public void setPatClosedGyno(boolean patClosedGyno) {
		this.patClosedGyno = patClosedGyno;
	}

	public boolean isPatClosedOther() {
		return patClosedOther;
	}

	public void setPatClosedOther(boolean patClosedOther) {
		this.patClosedOther = patClosedOther;
	}

	public String getPatClosedNote() {
		return patClosedNote;
	}

	public void setPatClosedNote(String patClosedNote) {
		this.patClosedNote = patClosedNote;
	}

	public boolean isPatOpenNothing() {
		return patOpenNothing;
	}

	public void setPatOpenNothing(boolean patOpenNothing) {
		this.patOpenNothing = patOpenNothing;
	}

	public boolean isPatOpenHypertension() {
		return patOpenHypertension;
	}

	public void setPatOpenHypertension(boolean patOpenHypertension) {
		this.patOpenHypertension = patOpenHypertension;
	}

	public boolean isPatOpenDrugaddiction() {
		return patOpenDrugaddiction;
	}

	public void setPatOpenDrugaddiction(boolean patOpenDrugaddiction) {
		this.patOpenDrugaddiction = patOpenDrugaddiction;
	}

	public boolean isPatOpenCardiovascular() {
		return patOpenCardiovascular;
	}

	public void setPatOpenCardiovascular(boolean patOpenCardiovascular) {
		this.patOpenCardiovascular = patOpenCardiovascular;
	}

	public boolean isPatOpenInfective() {
		return patOpenInfective;
	}

	public void setPatOpenInfective(boolean patOpenInfective) {
		this.patOpenInfective = patOpenInfective;
	}

	public boolean isPatOpenEndocrinometabol() {
		return patOpenEndocrinometabol;
	}

	public void setPatOpenEndocrinometabol(boolean patOpenEndocrinometabol) {
		this.patOpenEndocrinometabol = patOpenEndocrinometabol;
	}

	public boolean isPatOpenRespiratory() {
		return patOpenRespiratory;
	}

	public void setPatOpenRespiratory(boolean patOpenRespiratory) {
		this.patOpenRespiratory = patOpenRespiratory;
	}

	public boolean isPatOpenCancer() {
		return patOpenCancer;
	}

	public void setPatOpenCancer(boolean patOpenCancer) {
		this.patOpenCancer = patOpenCancer;
	}

	public boolean isPatOpenOrto() {
		return patOpenOrto;
	}

	public void setPatOpenOrto(boolean patOpenOrto) {
		this.patOpenOrto = patOpenOrto;
	}

	public boolean isPatOpenGyno() {
		return patOpenGyno;
	}

	public void setPatOpenGyno(boolean patOpenGyno) {
		this.patOpenGyno = patOpenGyno;
	}

	public boolean isPatOpenOther() {
		return patOpenOther;
	}

	public void setPatOpenOther(boolean patOpenOther) {
		this.patOpenOther = patOpenOther;
	}

	public String getPatOpenNote() {
		return patOpenNote;
	}

	public void setPatOpenNote(String patOpenNote) {
		this.patOpenNote = patOpenNote;
	}

	public String getPatSurgery() {
		return patSurgery;
	}

	public void setPatSurgery(String patSurgery) {
		this.patSurgery = patSurgery;
	}

	public String getPatAllergy() {
		return patAllergy;
	}

	public void setPatAllergy(String patAllergy) {
		this.patAllergy = patAllergy;
	}

	public String getPatTherapy() {
		return patTherapy;
	}

	public void setPatTherapy(String patTherapy) {
		this.patTherapy = patTherapy;
	}

	public String getPatMedicine() {
		return patMedicine;
	}

	public void setPatMedicine(String patMedicine) {
		this.patMedicine = patMedicine;
	}

	public String getPatNote() {
		return patNote;
	}

	public void setPatNote(String patNote) {
		this.patNote = patNote;
	}

	public boolean isPhyNutritionNormal() {
		return phyNutritionNormal;
	}

	public void setPhyNutritionNormal(boolean phyNutritionNormal) {
		this.phyNutritionNormal = phyNutritionNormal;
	}

	public String getPhyNutritionAbnormal() {
		return phyNutritionAbnormal;
	}

	public void setPhyNutritionAbnormal(String phyNutritionAbnormal) {
		this.phyNutritionAbnormal = phyNutritionAbnormal;
	}

	public boolean isPhyBowelNormal() {
		return phyBowelNormal;
	}

	public void setPhyBowelNormal(boolean phyBowelNormal) {
		this.phyBowelNormal = phyBowelNormal;
	}

	public String getPhyBowelAbnormal() {
		return phyBowelAbnormal;
	}

	public void setPhyBowelAbnormal(String phyBowelAbnormal) {
		this.phyBowelAbnormal = phyBowelAbnormal;
	}

	public boolean isPhyDiuresisNormal() {
		return phyDiuresisNormal;
	}

	public void setPhyDiuresisNormal(boolean phyDiuresisNormal) {
		this.phyDiuresisNormal = phyDiuresisNormal;
	}

	public String getPhyDiuresisAbnormal() {
		return phyDiuresisAbnormal;
	}

	public void setPhyDiuresisAbnormal(String phyDiuresisAbnormal) {
		this.phyDiuresisAbnormal = phyDiuresisAbnormal;
	}

	public boolean isPhyAlcohol() {
		return phyAlcohol;
	}

	public void setPhyAlcohol(boolean phyAlcohol) {
		this.phyAlcohol = phyAlcohol;
	}

	public boolean isPhySmoke() {
		return phySmoke;
	}

	public void setPhySmoke(boolean phySmoke) {
		this.phySmoke = phySmoke;
	}

	public boolean isPhyDrug() {
		return phyDrug;
	}

	public void setPhyDrug(boolean phyDrug) {
		this.phyDrug = phyDrug;
	}

	public boolean isPhyPeriodNormal() {
		return phyPeriodNormal;
	}

	public void setPhyPeriodNormal(boolean phyPeriodNormal) {
		this.phyPeriodNormal = phyPeriodNormal;
	}

	public String getPhyPeriodAbnormal() {
		return phyPeriodAbnormal;
	}

	public void setPhyPeriodAbnormal(String phyPeriodAbnormal) {
		this.phyPeriodAbnormal = phyPeriodAbnormal;
	}

	public boolean isPhyMenopause() {
		return phyMenopause;
	}

	public void setPhyMenopause(boolean phyMenopause) {
		this.phyMenopause = phyMenopause;
	}

	public int getPhyMenopauseYears() {
		return phyMenopauseYears;
	}

	public void setPhyMenopauseYears(int phyMenopauseYears) {
		this.phyMenopauseYears = phyMenopauseYears;
	}

	public boolean isPhyHrtNormal() {
		return phyHrtNormal;
	}

	public void setPhyHrtNormal(boolean phyHrtNormal) {
		this.phyHrtNormal = phyHrtNormal;
	}

	public String getPhyHrtAbnormal() {
		return phyHrtAbnormal;
	}

	public void setPhyHrtAbnormal(String phyHrtAbnormal) {
		this.phyHrtAbnormal = phyHrtAbnormal;
	}

	public boolean isPhyPregnancy() {
		return phyPregnancy;
	}

	public void setPhyPregnancy(boolean phyPregnancy) {
		this.phyPregnancy = phyPregnancy;
	}

	public int getPhyPregnancyNumber() {
		return phyPregnancyNumber;
	}

	public void setPhyPregnancyNumber(int phyPregnancyNumber) {
		this.phyPregnancyNumber = phyPregnancyNumber;
	}

	public int getPhyPregnancyBirth() {
		return phyPregnancyBirth;
	}

	public void setPhyPregnancyBirth(int phyPregnancyBirth) {
		this.phyPregnancyBirth = phyPregnancyBirth;
	}

	public int getPhyPregnancyAbort() {
		return phyPregnancyAbort;
	}

	public void setPhyPregnancyAbort(int phyPregnancyAbort) {
		this.phyPregnancyAbort = phyPregnancyAbort;
	}

	public int getLock() {
		return lock;
	}

	public void setLock(int lock) {
		this.lock = lock;
	}
}

package org.grameen.fdp.syncObjects;

import com.google.gson.annotations.Expose;

/**
 * Created by aangjnr on 24/03/2018.
 */

public class Farmer {
    String submitAgreement__c;
    String External_id__c;
    String fullName__c;
    String gender__c;
    String Submission__c;
    String village__c;
    String Name;
    String birthday__c;
    String educationalLevel__c;
    String lastVisitDate__c;
    String lastModifiedDate__c;
    String firstVisitDate__c;
    String photoUrl__c;


    public Farmer() {
    }


    public void setFirstVisitDate__c(String firstVisitDate__c) {
        this.firstVisitDate__c = firstVisitDate__c;
    }

    public String getFirstVisitDate__c() {
        return firstVisitDate__c;
    }

    public void setPhotoUrl__c(String photoUrl__c) {
        this.photoUrl__c = photoUrl__c;
    }

    public String getPhotoUrl__c() {
        return photoUrl__c;
    }

    public void setSubmitAgreement__c(String submitAgreement__c) {
        this.submitAgreement__c = submitAgreement__c;
    }

    public String getSubmitAgreement__c() {
        return submitAgreement__c;
    }

    public void setLastModifiedDate__c(String lastModifiedDate__c) {
        this.lastModifiedDate__c = lastModifiedDate__c;
    }

    public void setLastVisitDate__c(String lastVisitDate__c) {
        this.lastVisitDate__c = lastVisitDate__c;
    }

    public String getLastModifiedDate__c() {
        return lastModifiedDate__c;
    }

    public String getLastVisitDate__c() {
        return lastVisitDate__c;
    }

    public void setBirthday__c(String birthday__c) {
        this.birthday__c = birthday__c;
    }

    public void setEducationalLevel__c(String educationalLevel__c) {
        this.educationalLevel__c = educationalLevel__c;
    }

    public String getBirthday__c() {
        return birthday__c;
    }

    public String getEducationalLevel__c() {
        return educationalLevel__c;
    }

    public void setExternal_id__c(String external_id__c) {
        External_id__c = external_id__c;
    }

    public void setFullName__c(String fullName__c) {
        this.fullName__c = fullName__c;
    }

    public void setGender__c(String gender__c) {
        this.gender__c = gender__c;
    }

    public void setSubmission__c(String submission__c) {
        Submission__c = submission__c;
    }

    public void setVillage__c(String village__c) {
        this.village__c = village__c;
    }

    public String getExternal_id__c() {
        return External_id__c;
    }

    public String getFullName__c() {
        return fullName__c;
    }

    public String getGender__c() {
        return gender__c;
    }

    public String getSubmission__c() {
        return Submission__c;
    }

    public String getVillage__c() {
        return village__c;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public String getName() {
        return Name;
    }
}

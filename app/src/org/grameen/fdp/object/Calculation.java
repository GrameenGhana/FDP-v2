package org.grameen.fdp.object;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aangjnr on 04/01/2018.
 */

public class Calculation {


    @SerializedName("Id")
    String id;

    @SerializedName("Name")
    String name;

    @SerializedName("Question_1__c")
    String question1;


    @SerializedName("Operator_1__c")
    String operator1;

    @SerializedName("Question_2__c")
    String question2;

    @SerializedName("Operator_2__c")
    String operator2;

    @SerializedName("Question_3__c")
    String question3;

    @SerializedName("Operator_3__c")
    String operator3;

    @SerializedName("Question_4__c")
    String question4;

    @SerializedName("Result_question__c")
    String resultQuestion;




    public Calculation(){}


    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOperator1(String operator1) {
        this.operator1 = operator1;
    }

    public void setOperator2(String operator2) {
        this.operator2 = operator2;
    }

    public void setOperator3(String operator3) {
        this.operator3 = operator3;
    }

    public void setQuestion1(String question1) {
        this.question1 = question1;
    }

    public void setQuestion2(String question2) {
        this.question2 = question2;
    }

    public void setQuestion3(String question3) {
        this.question3 = question3;
    }

    public void setQuestion4(String question4) {
        this.question4 = question4;
    }

    public void setResultQuestion(String resultQuestion) {
        this.resultQuestion = resultQuestion;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOperator1() {
        return operator1;
    }

    public String getOperator2() {
        return operator2;
    }

    public String getOperator3() {
        return operator3;
    }

    public String getQuestion1() {
        return question1;
    }

    public String getQuestion2() {
        return question2;
    }

    public String getQuestion3() {
        return question3;
    }

    public String getQuestion4() {
        return question4;
    }

    public String getResultQuestion() {
        return resultQuestion;
    }








}

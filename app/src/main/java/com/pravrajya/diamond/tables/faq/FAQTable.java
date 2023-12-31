package com.pravrajya.diamond.tables.faq;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class FAQTable extends RealmObject{


    @PrimaryKey
    private String question;
    private String answer;

    public FAQTable() {
    }

    public FAQTable(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }


    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}

// src/main/java/com/example/GoCafe/dto/OnboardingForm.java
package com.example.GoCafe.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class QuestionsForm {
    private String purpose; // work_study | date | chatting | solo_coffee | meeting | photo_spot | with_kids
    private String vibe;    // modern | cozy | retro | industrial | nature
    private String factor;  // coffee | atmosphere | location | price | dessert
    private String time;    // morning | afternoon | evening | weekend
    private String area;    // hongdae | hapjeong | sangsu | anywhere
}

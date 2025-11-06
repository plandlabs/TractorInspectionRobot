package kr.re.kitech.tractorinspectionrobot.views.recyclerView.steps.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StepsItem {
    private int stepNum;
    private String subject;
    private int stepValue;
    private boolean stepValueBool;
    private boolean stepWorkingBool;
    private int angle;
    private boolean angleBtn = false;

    public StepsItem(int stepNum, String subject, int stepValue) {
        this.stepNum = stepNum;
        this.subject = subject;
        this.stepValue = stepValue;
    }
    public StepsItem(int stepNum, String subject, int stepValue, int angle) {
        this.stepNum = stepNum;
        this.subject = subject;
        this.stepValue = stepValue;
        this.angle = angle;
        this.angleBtn = true;
    }
}

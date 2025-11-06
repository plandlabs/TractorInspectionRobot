package kr.re.kitech.tractorinspectionrobot.mqtt.shared;

import lombok.Setter;

@Setter
public class SharedMqttViewModelBridge {
    private static final SharedMqttViewModelBridge INSTANCE = new SharedMqttViewModelBridge();
    private SharedMqttViewModel viewModel;

    private SharedMqttViewModelBridge() {}

    public static SharedMqttViewModelBridge getInstance() {
        return INSTANCE;
    }

    public void postDirectMessage(String topic, String payload) {
        if (viewModel != null) {
            viewModel.postDirectMessage(topic, payload);
        }
    }
}

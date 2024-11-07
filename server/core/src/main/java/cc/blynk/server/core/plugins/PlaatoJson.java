package cc.blynk.server.core.plugins;

import cc.blynk.server.core.model.serialization.JsonParser;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class PlaatoJson {

    @JsonProperty("device_id")
    public final int deviceId;

    @JsonProperty("device_name")
    public final String deviceName;

    public final int bpm;

    public final float temp;

    public final Float sg;

    @JsonProperty("co2_volume")
    public final Float co2Volume;

    public final int bubbles;

    @JsonProperty("batch_volume")
    public final Float batchVolume;

    public final Float og;

    public final Float abv;

    @JsonProperty("temp_unit")
    public final String tempUnit;

    @JsonProperty("volume_unit")
    public final String volumeUnit;

    public PlaatoJson(int deviceId, String deviceName,
                      int bpm, float temp,
                      Float sg, Float co2Volume,
                      int bubbles, Float batchVolume,
                      Float og, Float abv,
                      String tempUnit, String volumeUnit) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.bpm = bpm;
        this.temp = temp;
        this.sg = sg;
        this.co2Volume = co2Volume;
        this.bubbles = bubbles;
        this.batchVolume = batchVolume;
        this.og = og;
        this.abv = abv;
        this.tempUnit = tempUnit;
        this.volumeUnit = volumeUnit;
    }

    public PlaatoJson(int deviceId, String deviceName, PlaatoStructure plaato) {
        this(deviceId, deviceName,
                plaato.getBpm(), plaato.getTemp(),
                plaato.getSG(), plaato.getCo2Volume(),
                plaato.getBubbles(), plaato.getBatchVolume(),
                plaato.getOG(), plaato.getABV(),
                plaato.getTemperatureUnits(), plaato.getVolumeUnits());
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}

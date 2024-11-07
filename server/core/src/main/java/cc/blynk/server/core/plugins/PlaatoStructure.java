package cc.blynk.server.core.plugins;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
// Every provisioned PLAATO-device will have one instance of virtual_plaato running on
// the Blynk server, conserving states, doing simple calculations and routing
// traffic between the device and the server.

// UNITS:
// All stored variables and calculations are done using SI units (Celsius and Liters)
// (However.. The default units IN THE APP are Fahrenheit and US gal - due to.. well, Americans..)

public class PlaatoStructure {


    private volatile int bpm;                            // Bubbles Per Minute (indication of fermentation activity)
    private volatile float temperature;                    // Temperature in Celsius

    private volatile boolean isVolumeSet = false;
    private volatile float volume;                        // Volume in Liters

    private volatile boolean isOGSet = false;
    private volatile float og;                            // Original gravity of beer

    private volatile boolean isSGSet = false;
    private volatile float sg;                            // Specific gravity of beer

    private volatile boolean isABVSet = false;
    private volatile float abv;                           // Alcohol By Volume [%]

    private volatile boolean isCo2Set = false;
    private volatile float co2;                           // CO2 volume [L]

    private volatile int temperatureUnit = 1;            // 1 - Fahrenheit - default display value for app
    private volatile int volumeUnit = 1;                // 1 - US gallons - default input/display value for app
    private volatile int bubbles = 0;            // How many bubbles has passed through airlock during fermentation
    private volatile int espBubbleCount;    // Local bubble count in ESP8266
    private volatile long lastESPTransmission;

    private volatile boolean learning = false;              // Flag to enable gravity estimation learning
    private volatile int bubPerGrav = 600000;               // Bubbles per gravity-drop per volume
    private volatile boolean batchReadyForLearning = true;

    /**
     * Sets values for reserved virtual pins by hardware
     *
     * @param pin 0 for bubbles 1 for temperature. Rest is ignored.
     */
    public void setHardwarePinData(short pin, String data) {
        if (pin == 100) {
            bubblesReceived(data);
        } else if (pin == 101) {
            temperatureReceived(data);
        }
    }

    /**
     * Retrieve various Plaato attributes mapped to pins
     *
     * @param pin 0 for bubbles 1 for temperature. Rest is ignored.
     */
    public String pullPinData(short pin) {
        switch (pin) {
            case 102:
                //send "--" if no new measurements within last 16 minutes
                return (System.currentTimeMillis() - lastESPTransmission) < 960000L ? String.valueOf(bpm) : "--";
            case 103:
                //send "--" if no new measurements within last 16 minutes
                if (System.currentTimeMillis() - lastESPTransmission < 960000L) {
                    //convert to Fahrenheit if needed
                    return String.valueOf(temperatureUnit == 2 ? temperature : temperature * 1.8 + 32);
                } else {
                    return "--";
                }
            case 104:
                //convert to US gallon if needed
                return isVolumeSet ? String.valueOf(volumeUnit == 2 ? volume : volume * 0.264172) : null;
            case 105:
                return isOGSet ? String.valueOf(og) : null;
            case 106:
                return isSGSet ? String.valueOf(sg) : null;
            case 107:
                return isABVSet ? String.valueOf(abv) : null;
            case 108:
                return getTemperatureUnits();
            case 109:
                return getVolumeUnits();
            case 110:
                return String.valueOf(bubbles);             // Used for debugging
            case 118:
                return String.valueOf(bubPerGrav);          // Used for debugging
            case 119:
                //convert to US gallon if needed
                return isCo2Set ? String.valueOf(volumeUnit == 2 ? co2 : co2 * 0.264172) : null;
            default:
                return null;
        }
    }

    /**
     * Defines various Plaato attributes mapped to pins by app
     *
     * @param pin   virtual pin number
     * @param value new attribute value
     */
    public void setAppPinData(short pin, int value) {
        switch (pin) {
            case 111:
                setVolume(value);
                break;
            case 112:
                setOriginalGravity(value);
                break;
            case 113:
                setSpecificGravity(value);
                break;
            case 114:
                this.temperatureUnit = value;
                break;
            case 115:
                setVolumeUnit(value);
                break;
            case 116:
                reset(value);
                break;
            case 117:
                setLearning(value);
                break;
        }
    }

    public static boolean isReservedByApp(DataStream dataStream) {
        return dataStream != null && isReservedByApp(dataStream.pinType, dataStream.pin);
    }

    /**
     * Pins reserved by Plaato app are handled differently from other pins
     *
     * @param pinType only virtual pins are reserved
     * @param pin     pin no.
     * @return is pin is reserved for Plaato app
     */
    public static boolean isReservedByApp(PinType pinType, short pin) {
        return PinType.VIRTUAL == pinType && pin > 101 && pin < 120;
    }

    /**
     * Pins reserved by Plaato hardware are handled differently from other pins
     *
     * @param pinType only virtual pins are reserved
     * @param pin     pin no.
     * @return is pin is reserved for Plaato hardware
     */
    public static boolean isReservedByHardware(PinType pinType, int pin) {
        return PinType.VIRTUAL == pinType && (pin == 100 || pin == 101);
    }

    /**
     * Called when data is received on V0 from ESP8266
     *
     * @param newBubbles new bubbles amount
     */
    private void bubblesReceived(String newBubbles) {
        int newBubblesParsed = Integer.parseInt(newBubbles);
        if (lastESPTransmission > 0) {
            recalcBubblesAndBPM(newBubblesParsed);
            // Set timestamp for last received data from esp8266
            estimateSgAndAbv();
        } else {
            // EDGE CASE: First time data is received
            this.espBubbleCount = newBubblesParsed;
        }
        lastESPTransmission = System.currentTimeMillis();
    }

    /**
     * Updates the cloud variables 'bubbles' abd 'bpm' based on the previous and new bubble count from the esp8266
     *
     * @param newBubbles new bubbles amount
     */
    private void recalcBubblesAndBPM(int newBubbles) {
        int diff;

        // Find how many bubbles since last update
        if (newBubbles < this.espBubbleCount) { // Check if bubble count on hardware has been reset
            diff = newBubbles;
        } else {
            diff = newBubbles - this.espBubbleCount;
        }

        // Calculate bubbles per minute.
        // safe assumption that there will be no 500 years interval between 2 transmissions
        if (lastESPTransmission > 0) {
            float period = (float) (System.currentTimeMillis() - lastESPTransmission) / 60000;
            this.bpm = period > 0 ? Math.round((diff / period)) : 0;
        }
        // Update bubbles
        this.espBubbleCount = newBubbles;
        this.bubbles += diff;
    }

    /**
     * Called when data is received on v1 from ESP8266
     *
     * @param newTemperature new temperature
     */
    private void temperatureReceived(String newTemperature) {
        this.temperature = pcbHeatCorrection(Float.parseFloat(newTemperature));
    }

    /**
     * PCB self-heating correction. Compansating for heat generated in Plaato PCB
     *
     * @param pcbTemperature pcb temperature
     */
    private float pcbHeatCorrection(float pcbTemperature) {
        return (0.9133f * pcbTemperature) - 1.145f;
    }

    /**
     * V8. Used to display the chosen temperature unit in a Value Display
     *
     * @return volume units
     */
    String getTemperatureUnits() {
        if (temperatureUnit == 1) {
            return "°F";
        }
        return "°C";
    }

    /**
     * V9. Used to display the chosen volume unit in a Value Display
     *
     * @return volume units
     */
    String getVolumeUnits() {
        if (volumeUnit == 1) {
            return "gal";
        }
        return "L";
    }

    /**
     * V11
     *
     * @param volume new volume
     */
    private void setVolume(int volume) {
        // Map from Menu index to float
        float userVolume = (float) volume / 10;
        if (volumeUnit == 2) {            // Liters
            this.volume = userVolume;
        } else {                            // Assume US gallons
            this.volume = userVolume / 0.264172f;
        }
        this.isVolumeSet = true;
        estimateSgAndAbv();
    }

    /**
     * V12
     *
     * @param originalGravity new original gravity
     */
    private void setOriginalGravity(int originalGravity) {
        // Map from Menu index to float
        this.og = (float) (originalGravity - 1) / 1000 + 1;
        this.isOGSet = true;
        estimateSgAndAbv();
    }

    /**
     * V13
     *
     * @param specificGravity new specific gravity
     */
    private void setSpecificGravity(int specificGravity) {
        // Map from Menu index to float
        float newSpecificGravity = (float) (specificGravity - 1) / 1000 + 1;
        float denom = (og - newSpecificGravity) * volume;
        int newBubPerGrav = 0;
        if (denom > 0) {
            newBubPerGrav = (int) (bubbles / denom);
        }

        // If learning is enabled, new sg value is reasonable and
        // no sg-update without learning has been done in batch,
        // update bubPerGrav instead of bubbles.
        if (this.learning && this.batchReadyForLearning && newBubPerGrav > 300000 && newBubPerGrav < 1000000) {
            this.bubPerGrav = newBubPerGrav;
        } else {
            this.batchReadyForLearning = false;
            // SG can not be higher than OG
            if (og < newSpecificGravity) {
                bubbles = 0;
            } else {
                bubbles = (int) ((og - newSpecificGravity) * bubPerGrav * volume);
            }
        }
        estimateSgAndAbv();
    }

    /**
     * V115
     *
     * @param newVolumeUnit unit new volume unit
     */
    private void setVolumeUnit(int newVolumeUnit) {
        if (this.volumeUnit == 1 && newVolumeUnit == 2) {
            // Volume unit changed from US gallons to Liters.
            // Backwards conversion since volume is stored in Liters.
            this.volume = this.volume * 0.264172F;
        } else if (this.volumeUnit == 2 && newVolumeUnit == 1) {
            // Volume unit changed from Liters to US gallons.
            // Backwards conversion since volume is stored in Liters.
            this.volume = this.volume / 0.264172F;
        }
        // Set new volumeUnit
        this.volumeUnit = newVolumeUnit;
    }

    /**
     * V116. Resets the bubble counting.
     *
     * @param resetIndex weird number to reset
     */
    private void reset(int resetIndex) {
        if (resetIndex == 1) {
            bubbles = 0;
            estimateSgAndAbv();
            this.batchReadyForLearning = true;
        }
    }

    /**
     * V117. Enables gravity estimation learning from user
     *
     * @param learningIndex
     */
    private void setLearning(int learningIndex) {
        if (learningIndex == 1) {
            this.learning = true;
        } else {
            this.learning = false;
        }
    }

    /**
     * Updates cloud variables 'sg' and 'abv'
     */
    private void estimateSgAndAbv() {
        if (isVolumeSet && isOGSet) {
            this.sg = this.og - ((float) (bubbles) / volume) / bubPerGrav;
            this.isSGSet = true;
            this.abv = 76.08f * (this.og - this.sg) / (1.775f - this.og) * this.sg / 0.794f;
            this.isABVSet = true;
            this.co2 = (this.og - this.sg) * 1000F * volume * 1.041F / 1.842F;
            this.isCo2Set = true;
        }
    }

    int getBpm() {
        return bpm;
    }

    float getTemp() {
        return temperatureUnit == 2 ? temperature : temperature * 1.8F + 32;
    }

    Float getSG() {
        return isSGSet ? sg : null;
    }

    Float getCo2Volume() {
        return isCo2Set ? (volumeUnit == 2 ? co2 : co2 * 0.264172F) : null;
    }

    int getBubbles() {
        return bubbles;
    }

    Float getBatchVolume() {
        return isVolumeSet ? (volumeUnit == 2 ? volume : volume * 0.264172F) : null;
    }

    Float getOG() {
        return isOGSet ? og : null;
    }

    Float getABV() {
        return isABVSet ? abv : null;
    }

}

package uzmany.bmonitor;
/**
 * Created by Usman on 2016-06-21.
 */


import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Dave Smith
 * Double Encore, Inc.
 * SensorTagData
 * Conversions for sensor data values on the TI SensorTag
 */
public class SensorTagData {

    // MOVEMENT DATA EXTRACTION
    public static double extractMov(BluetoothGattCharacteristic c) {
        int a = shortUnsignedAtOffset(c, 2);
        // bits [1..0] are status bits and need to be cleared
        a = a - (a % 4);

        return ((-6f) + 125f * (a / 65535f));

    }

    public static Point3D extractMov_Acc(final byte[] value) {
        final float SCALE = (float) 4096.0;

        int x = (value[7]<<8) + value[6];
        int y = (value[9]<<8) + value[8];
        int z = (value[11]<<8) + value[10];
        return new Point3D(((x / SCALE) * -1), y / SCALE, ((z / SCALE)*-1));
    }
    public static Point3D extractMov_Gyro(final byte[] value) {
        final float SCALE = (float) 128.0;

        int x = (value[1]<<8) + value[0];
        int y = (value[3]<<8) + value[2];
        int z = (value[5]<<8) + value[4];
        return new Point3D(x / SCALE, y / SCALE, z / SCALE);


    }
    public static Point3D extractMov_Mag(final byte[] value) {
        final float SCALE = (float) (32768 / 4912);
        if (value.length >= 18) {
            int x = (value[13]<<8) + value[12];
            int y = (value[15]<<8) + value[14];
            int z = (value[17]<<8) + value[16];
            return new Point3D(x / SCALE, y / SCALE, z / SCALE);
        }
        else return new Point3D(0,0,0);
    }




    //END MOVEMENT DATA EXTRACTION
    public static double extractHumAmbientTemperature(BluetoothGattCharacteristic c) {
        int rawT = shortSignedAtOffset(c, 0);

        return -46.85 + 175.72/65536 *(double)rawT;
    }

    public static double extractHumidity(BluetoothGattCharacteristic c) {
        int a = shortUnsignedAtOffset(c, 2);
        // bits [1..0] are status bits and need to be cleared
        a = a - (a % 4);

        return ((-6f) + 125f * (a / 65535f));

    }
    public static double extractLux(BluetoothGattCharacteristic c) {
        int mantissa;
        int exponent;
        Integer sfloat= shortUnsignedAtOffset(c, 0);

        mantissa = sfloat & 0x0FFF;
        exponent = (sfloat >> 12) & 0xFF;

        double output;
        double magnitude = Math.pow(2.0f, exponent);
        output = (mantissa * magnitude);

        return (output / 100.0f);
    }

    public static int[] extractCalibrationCoefficients(BluetoothGattCharacteristic c) {
        int[] coefficients = new int[8];

        coefficients[0] = shortUnsignedAtOffset(c, 0);
        coefficients[1] = shortUnsignedAtOffset(c, 2);
        coefficients[2] = shortUnsignedAtOffset(c, 4);
        coefficients[3] = shortUnsignedAtOffset(c, 6);
        coefficients[4] = shortSignedAtOffset(c, 8);
        coefficients[5] = shortSignedAtOffset(c, 10);
        coefficients[6] = shortSignedAtOffset(c, 12);
        coefficients[7] = shortSignedAtOffset(c, 14);

        return coefficients;
    }

    public static double extractBarTemperature(BluetoothGattCharacteristic characteristic, final int[] c) {
        // c holds the calibration coefficients

        int t_r;	// Temperature raw value from sensor
        double t_a; 	// Temperature actual value in unit centi degrees celsius

        t_r = shortSignedAtOffset(characteristic, 0);

        t_a = (100 * (c[0] * t_r / Math.pow(2,8) + c[1] * Math.pow(2,6))) / Math.pow(2,16);

        return t_a / 100;
    }

    public static double extractBarometer(BluetoothGattCharacteristic characteristic, final int[] c) {
        // c holds the calibration coefficients

        int t_r;	// Temperature raw value from sensor
        int p_r;	// Pressure raw value from sensor
        double S;	// Interim value in calculation
        double O;	// Interim value in calculation
        double p_a; 	// Pressure actual value in unit Pascal.

        t_r = shortSignedAtOffset(characteristic, 0);
        p_r = shortUnsignedAtOffset(characteristic, 2);


        S = c[2] + c[3] * t_r / Math.pow(2,17) + ((c[4] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,19);
        O = c[5] * Math.pow(2,14) + c[6] * t_r / Math.pow(2,3) + ((c[7] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,4);
        p_a = (S * p_r + O) / Math.pow(2,14);

        //Convert pascal to in. Hg
        double p_hg = p_a * 0.000296;

        return p_hg;
    }

    /**
     * Gyroscope, Magnetometer, Barometer, IR temperature
     * all store 16 bit two's complement values in the awkward format
     * LSB MSB, which cannot be directly parsed as getIntValue(FORMAT_SINT16, offset)
     * because the bytes are stored in the "wrong" direction.
     *
     * This function extracts these 16 bit two's complement values.
     * */
    private static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.

        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }
}



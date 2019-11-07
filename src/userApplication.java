package com.company;

import javax.sound.sampled.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import static java.lang.System.exit;
/**
 * The UserApplication program implements an application that
 * establish connection with Ithaki and by using UDP protocol (and TCP)
 * request and receive echo packages, images, audio clips, Copter Telemetry and On Board Diagnostics
 *
 * @author  George Balaouras
 * @version 1.0 {final}
 * @since   20-02-2019
 */
public class UserApplication {

    private static final int FirstHalfMask = 0b11110000;
    private static final int SecondHalfMask = 0b00001111;
    private static final int MeanSignMask = 0b10000000;
    private static final int StepSignMask = 0b10000000;

    private static final int serverPort = 38015;
    private static final int clientPort = 48015;

    private static final int TOTAL_SOUND_CLIPS = 999;
    private static final String[] Vehicle_Operations = new String[] { "01 1F", "01 0F",
                                                                      "01 11", "01 0C",
                                                                      "01 0D", "01 05" };
    private  static  final int EngineRunTime = 0;
    private  static  final int IntakeAirTemperature = 1;
    private  static  final int ThrottlePosition = 2;
    private  static  final int EngineRPM = 3;
    private  static  final int VehicleSpeed = 4;
    private  static  final int CoolantTemperature = 5;
    private  static  final int TemperatureOFF = 1;
    private  static  final int TemperatureON = 2;
    private  static  final int CAMFIX = 1;
    private  static  final int CAMPTZ = 2;
    private  static  final String FOLDERNAME = "Session1";

    private static final String echo_request_code =  "E7468";
    private static final String img_request_code =   "M0324";
    private static final String sound_request_code = "A0795";
    private static final String Ithaki_copter_code = "Q7032";
    private static final String OBD_request_code =   "V4820";

    private static FileOutputStream img_stream;
    private static FileOutputStream echo_stream;
    private static FileOutputStream time_stream;

    public static void main(String[] args) throws IOException, LineUnavailableException {
        File directory = new File(FOLDERNAME);
        if (! directory.exists()){
            directory.mkdir();
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        System.out.println("The Session has started at " + dtf.format(LocalDateTime.now()));
        while(true) {
            ParseInput();
        }
    }

    private static void EchoPackages(int Temp) throws IOException {
        String echo_req = "";
        //File DelayOff = new File(FOLDERNAME + "/Echo_Without_Delay.csv");     // Uncomment when echo code = E0000
        //FileOutputStream delayOffstream = new FileOutputStream(DelayOff);     // Uncomment when echo code = E0000
        //File Delay = new File(FOLDERNAME + "/Echo_Delay.csv");                // Uncomment to find delay
        //FileOutputStream delaystream = new FileOutputStream(Delay);           // Uncomment to find delay
        if (Temp == 1) {               // Without Temp
            File Output_from_echo = new File(FOLDERNAME + "/Echo_Without_T.txt"); //Comment when echo code  = E0000
            File Time_for_echo = new File(FOLDERNAME + "/Echo_With_Delay.csv"); // && to find delay
            echo_stream = new FileOutputStream(Output_from_echo);
            time_stream = new FileOutputStream(Time_for_echo);
            echo_req = echo_request_code;
            //echo_req = "E0000\r";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.println(dtf.format(LocalDateTime.now()));
            System.out.println("Ongoing Operation: Echo packages (Without Temperature). Echo_code: " + echo_req);
        } else if (Temp == 2) {         // With Temp
            File Output_from_echo_With_T = new File(FOLDERNAME + "/Echo_With_T.txt");
            File Time_for_echo_With_T = new File(FOLDERNAME + "/Time_With_T.csv");
            echo_stream = new FileOutputStream(Output_from_echo_With_T);
            time_stream = new FileOutputStream(Time_for_echo_With_T);
            echo_req = echo_request_code + "T00\r";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.println(dtf.format(LocalDateTime.now()));
            System.out.println("Ongoing Operation: Echo packages (With Temperature). Echo_code: " + echo_req);
        } else {
            System.err.println("Type must be 1 (without Temp) or 2 (with Temp) ");
            exit(-1);
        }
        byte[] txbuffer;
        InetAddress hostAddress = InetAddress.getByName("155.207.18.208");
        DatagramSocket s = new DatagramSocket();
        txbuffer = echo_req.getBytes();
        DatagramSocket r = new DatagramSocket(clientPort);
        r.setSoTimeout(8000);
        byte[] rxbuffer = new byte[2048];
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);

        long Begin = System.currentTimeMillis();
        long start, end;
        String response = "", echostring = "", delaystring = "", delay = "";
        int packages = 0;

        while (System.currentTimeMillis() - Begin < 300000) { //3000 for T00
            String inside_text;
            start = System.currentTimeMillis();
            //txbuffer = ( packages % 2 == 0) ? echo_req.getBytes() : "E0000".getBytes();                //Uncomment to find the Delay
            //DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort); //Uncomment to find the Delay
            s.send(p);
            DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);
            try {
                r.receive(q);
                inside_text = new String(rxbuffer, 0, q.getLength());
                packages += 1;
                end = System.currentTimeMillis();
                //delaystring += (end - start) + ",";           // Uncomment when echo code = E0000
                //delay += (end - start) + ",";                 // Uncomment to find delay
                response += end - start + ",";
                echostring += inside_text + "\r\n";
            } catch (Exception x) {
                System.err.println("(Echo) Package didn't arrive.");
            }
        }

        // Write Results to Files //
        try {
            // delayOffstream.write(delaystring.getBytes());     // Uncomment when echo code = E0000
            // delayOffstream.close();                           // Uncomment when echo code = E0000
            // delaystream.write(delay.getBytes());              // Uncomment to find delay
            // delaystream.close();                              // Uncomment to find delay
            echo_stream.write(echostring.getBytes());
            time_stream.write(response.getBytes());
            echo_stream.close();
            time_stream.close();
        } catch (IOException x) {
            System.out.println("(Echo) Failure saving the results.");
        }
        r.close();
    }

    private static void ImageRequest(int type) throws IOException {
        String img_req = "";
        if (type == 1) {               // FIX mode
            File image_FIX = new File(FOLDERNAME + "/image_fix.jpeg");
            img_stream = new FileOutputStream(image_FIX);
            img_req = img_request_code + "CAM=FIX" ;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.println(dtf.format(LocalDateTime.now()));
            System.out.println("Ongoing Operation: Image Receiving (MODE = FIX). Image_code: " + img_req);
        } else if (type == 2) {         // PTZ mode
            File image_PTZ = new File(FOLDERNAME + "/image_ptz.jpeg");
            img_stream = new FileOutputStream(image_PTZ);
            img_req = img_request_code + "CAM=PTZ";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.println(dtf.format(LocalDateTime.now()));
            System.out.println("Ongoing Operation: Image Receiving (MODE = PTZ). Image_code: " + img_req);
        } else {
            System.err.println("Type must be 1 (for FIX mode) or 2 (for PTZ mode) ");
            exit(1);
        }
        int prev_len, curr_len = 0, packages = 0;
        ArrayList<Byte> DP = new ArrayList<>();

        byte[] txbuffer;
        DatagramSocket s = new DatagramSocket();
        txbuffer = img_req.getBytes();
        InetAddress hostAddress = InetAddress.getByName("155.207.18.208");
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);
        DatagramSocket r = new DatagramSocket(clientPort);
        r.setSoTimeout(8000);
        byte[] rxbuffer = new byte[2048];
        s.send(p);
        do {
            prev_len = curr_len;
            DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);
            try {
                r.receive(q);
                packages++;
                curr_len = q.getLength();
                byte[] curr_array = q.getData();
                for (int i = 0; i < curr_len; i++) {
                    DP.add(curr_array[i]);
                }
            } catch (Exception x) {
                System.err.println("(Image) Package didn't arrive. ");
                exit(-1);
            }
        } while (curr_len >= prev_len);

        // Write Results to Files //
        try {
            img_stream.write(convertBytes(DP));
            img_stream.close();
        } catch (IOException x) {
            System.out.println("(Image) Failure saving the results.");
        }
        r.close();
    }

    private static void SoundRequestNonAdaptive(int QuantizerBits, String AudioSource, String Encoding) throws IOException, LineUnavailableException {

        if (CheckAudioInput(QuantizerBits, AudioSource, Encoding)) {
            String sound_req = sound_request_code + AudioSource  + TOTAL_SOUND_CLIPS ;
            File audio_generator;
            FileOutputStream samples_stream; FileOutputStream differences_stream;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.println(dtf.format(LocalDateTime.now()));
            System.out.println("Ongoing Operation: Audio Receiving (NON adaptive). Audio_code: " + sound_req);
            if ("T".equals(AudioSource)) {
                audio_generator = new File(FOLDERNAME + "/audio_gen.wav");
                File Audio_Samples = new File(FOLDERNAME + "/audio_gen_samples.csv");
                File Audio_Differences = new File(FOLDERNAME + "/audio_gen_diff.csv");
                samples_stream = new FileOutputStream(Audio_Samples);
                differences_stream = new FileOutputStream(Audio_Differences);

            }else {
                audio_generator = new File(FOLDERNAME + "/audio_music.wav");
                File Audio_Samples = new File(FOLDERNAME + "/audio_music_samples.csv");
                File Audio_Differences = new File(FOLDERNAME + "/audio_music_diff.csv");
                samples_stream = new FileOutputStream(Audio_Samples);
                differences_stream = new FileOutputStream(Audio_Differences);
            }


            byte[] txbuffer;
            DatagramSocket s = new DatagramSocket();
            txbuffer = sound_req.getBytes();
            InetAddress hostAddress = InetAddress.getByName("155.207.18.208");
            DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);
            DatagramSocket r = new DatagramSocket(clientPort);
            r.setSoTimeout(8000);
            byte[] rxbuffer = new byte[2048];
            s.send(p);

            byte[] curr_data;
            int packages = 0, curr_len, temp_niddle1, temp_niddle2, Sample1, Sample2, sum;
            double mean_value;
            String audio_samples = "", audio_diff = "";
            ArrayList<Integer> PreSamples = new ArrayList<>();
            ArrayList<Byte> Samples = new ArrayList<>();

            do {
                DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);
                try {
                    sum = 0;                        // For each new Package sum is 0
                    Sample2 = 0;                    // For each new Package Sample[0] is 0
                    PreSamples.add(Sample2);        // Add x_0 for each iteration
                    r.receive(q);
                    curr_len = q.getLength();
                    curr_data = q.getData();
                    // Get the niddles from the byte //
                    for (int pos = 0; pos < curr_len; pos++) {
                        temp_niddle1 = ((FirstHalfMask & curr_data[pos]) >> 4) - 8;
                        temp_niddle2 = (SecondHalfMask & curr_data[pos]) - 8;
                        audio_diff += temp_niddle1 + ",";
                        audio_diff += temp_niddle2 + ",";
                        Sample1 = temp_niddle1 + Sample2;
                        Sample2 = temp_niddle2 + Sample1;
                        PreSamples.add(Sample1);
                        PreSamples.add(Sample2);
                        sum = sum + Sample1 + Sample2;
                    }
                    mean_value = sum / ( 2 * curr_len);
                    for (int i = 0; i < PreSamples.size(); i++){
                        Samples.add((byte) (PreSamples.get(i) - mean_value));  // Mean Value must be 0, for each package
                        audio_samples += Samples.get(i) + ",";
                    }
                    PreSamples.clear(); // For each new loop, PreSamples are empty
                    packages++;
                } catch (Exception x) {
                    System.err.println("(Audio) Package didn't arrive. ");
                    exit(-1);
                }
            } while (packages < TOTAL_SOUND_CLIPS);
            r.close();
            AudioFormat NON_adaptive = new AudioFormat(8000, QuantizerBits, 1, true, false);

            SourceDataLine lineOut = AudioSystem.getSourceDataLine(NON_adaptive);
            lineOut.open(NON_adaptive,32000);
            lineOut.start();
            lineOut.write(convertBytes(Samples),0,256*packages);
            lineOut.stop();
            lineOut.close();

            try {
                samples_stream.write(audio_samples.getBytes());
                samples_stream.close();
                differences_stream.write(audio_diff.getBytes());
                differences_stream.close();
                ByteArrayInputStream Audio_Data = new ByteArrayInputStream(convertBytes(Samples));
                AudioInputStream Audio = new AudioInputStream(Audio_Data, NON_adaptive, Samples.size());
                AudioSystem.write(Audio, AudioFileFormat.Type.WAVE, audio_generator);
            } catch (IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }
        }
    }

    private static void SoundRequestAdaptive(int QuantizerBits, String AudioSource, String Encoding) throws IOException, LineUnavailableException {
        if (CheckAudioInput(QuantizerBits, AudioSource, Encoding)) {
            String sound_req = sound_request_code + Encoding + AudioSource + TOTAL_SOUND_CLIPS;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.println(dtf.format(LocalDateTime.now()));
            System.out.println("Ongoing Operation: Audio Receiving (Adaptive). Audio_code: " + sound_req);

            File audio_generator = new File(FOLDERNAME + "/audio_AQ.wav");
            File Audio_Samples = new File(FOLDERNAME + "/audio_AQ_samples.csv");
            File Audio_Differences = new File(FOLDERNAME + "/audio_AQ_diff.csv");
            FileOutputStream samples_stream = new FileOutputStream(Audio_Samples);
            FileOutputStream differences_stream = new FileOutputStream(Audio_Differences);
            File MEAN = new File(FOLDERNAME + "/mean.csv");
            File STEP = new File(FOLDERNAME + "/step.csv");
            FileOutputStream mean_stream = new FileOutputStream(MEAN);
            FileOutputStream step_stream = new FileOutputStream(STEP);

            DatagramSocket s = new DatagramSocket();
            byte[] txbuffer = sound_req.getBytes();
            InetAddress hostAddress = InetAddress.getByName("155.207.18.208");
            DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);
            DatagramSocket r = new DatagramSocket(clientPort);
            r.setSoTimeout(8000);
            byte[] rxbuffer = new byte[2048];
            s.send(p);

            int packages = 0, curr_len, temp_niddle1, temp_niddle2, Sample1, Sample2, helper;
            String audio_samples = "", audio_diff = "", mean = "", step = "";
            ArrayList<Byte> PreSamples = new ArrayList<>();
            ArrayList<Byte> Samples = new ArrayList<>();
            byte[] curr_data;
            byte[] temp = new byte[4];
            byte sign;
            do {
                DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);
                try {
                    helper = 0;
                    r.receive(q);
                    curr_len = q.getLength();
                    curr_data = q.getData();
                    // Get μ, β from the header //
                    sign = (byte)( ( curr_data[1] & MeanSignMask) !=0 ? 0xFF : 0x00); //converting byte[2] to integer
                    temp[3] = sign;
                    temp[2] = sign;
                    temp[1] = curr_data[1];
                    temp[0] = curr_data[0];
                    int mean_value = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    sign = (byte)( ( curr_data[3] & StepSignMask) !=0 ? 0xFF : 0x00);
                    temp[3] = sign;
                    temp[2] = sign;
                    temp[1] = curr_data[3];
                    temp[0] = curr_data[2];
                    int quan_step = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    mean += mean_value + ","; step += quan_step + ",";
                    // Get the niddles from the byte //
                    for (int pos = 4; pos < curr_len; pos++) {
                        temp_niddle1 =  (0x0000000F & curr_data[pos]) - 8 ;
                        temp_niddle2 = ((0x000000F0 & curr_data[pos]) >> 4 ) - 8;
                        audio_diff += temp_niddle1 + ","; audio_diff += temp_niddle2 + ",";
                        Sample1 = (temp_niddle2 * quan_step) + helper + mean_value;
                        Sample2 = (temp_niddle1 * quan_step) + (temp_niddle2 * quan_step) + mean_value;
                        helper = temp_niddle1 * quan_step;
                        PreSamples.add((byte) ( Sample1 & 0x000000FF));
                        PreSamples.add((byte) ((Sample1 & 0x0000FF00) >> 8));
                        PreSamples.add((byte) ( Sample2 & 0x000000FF));
                        PreSamples.add((byte) ((Sample2 & 0x0000FF00) >> 8));
                    }
                    for (int i = 0; i <  PreSamples.size(); i++){
                        Samples.add(PreSamples.get(i));
                        audio_samples += PreSamples.get(i) + ",";
                    }
                    PreSamples.clear();
                    packages++;
                }
                catch (Exception x) {
                    System.err.println("(Audio) package didnt arrive.");
                    exit(-1);
                }
            } while (packages < TOTAL_SOUND_CLIPS);

            AudioFormat adaptive = new AudioFormat(8000, 16, 1, true, false);

            SourceDataLine lineOut = AudioSystem.getSourceDataLine(adaptive);
            lineOut.open(adaptive,32000);
            lineOut.start();
            lineOut.write(convertBytes(Samples),0,256 * 2 * packages);
            lineOut.stop();
            lineOut.close();

            try {
                samples_stream.write(audio_samples.getBytes());
                samples_stream.close();
                differences_stream.write(audio_diff.getBytes());
                differences_stream.close();
                mean_stream.write(mean.getBytes());
                mean_stream.close();
                step_stream.write(step.getBytes());
                step_stream.close();
                ByteArrayInputStream Audio_Data = new ByteArrayInputStream(convertBytes(Samples));
                AudioInputStream Audio = new AudioInputStream(Audio_Data, adaptive, Samples.size());
                AudioSystem.write(Audio, AudioFileFormat.Type.WAVE, audio_generator);
            } catch (IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }

            r.close();
        }
    }

    private static void IthakiCopter(int level) throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        System.out.println(dtf.format(LocalDateTime.now()));
        File CopterTelemetry = new File(FOLDERNAME + "/CopterTelemetry.csv");
        FileOutputStream copter_stream = new FileOutputStream(CopterTelemetry);

        InetAddress hostAddress = InetAddress.getByName("155.207.18.208");
        Socket OutputSocket = new Socket(hostAddress, 38048);
        BufferedReader Input = new BufferedReader(new InputStreamReader(OutputSocket.getInputStream()));
        DataOutputStream Output = new DataOutputStream(OutputSocket.getOutputStream());

        String telemetry, TelemetryOutput = "";
        String LLL, RRR, AAA, TTTT, PPPP;
        int left = 200,right = 200;
        try {
            for(int times = 0; times < 300; times++) {
                telemetry = Input.readLine();
                Output.writeBytes("AUTO FLIGHTLEVEL=" + level + " LMOTOR=" + left + " RMOTOR=" + right + " PILOT \r\n");
                if (telemetry.contains("ITHAKICOPTER")) {
                    LLL = telemetry.substring(20, 23);
                    RRR = telemetry.substring(31, 34);
                    AAA = telemetry.substring(44, 47);
                    TTTT = telemetry.substring(60, 66);
                    PPPP = telemetry.substring(76, 83);
                    //System.out.println(LLL + " " + RRR + " " + AAA + " " + TTTT + " " + PPPP);
                    TelemetryOutput += LLL + "," + RRR + "," + AAA + "," + TTTT + "," + PPPP + "\r\n";
                }
            }
        }catch (Exception e){
            System.out.println(e);
        }
        try {
            copter_stream.write(TelemetryOutput.getBytes());
            copter_stream.close();
        } catch (IOException x) {
            System.out.println("(Copter) Failure saving the results.");
        }
    }

    private static void Onboard_Diagnostics(int Oper) throws  IOException{
        File Vehicle_Operation = null;
        String OBD_req;
        OBD_req = OBD_request_code + "OBD=" + Vehicle_Operations[Oper];
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        switch (Oper){
            case EngineRunTime:
                System.out.println(dtf.format(LocalDateTime.now()));
                System.out.println("Ongoing Operation: OBD packages (Engine Run Time). OBD_code: " + OBD_req);
                Vehicle_Operation = new File(FOLDERNAME + "/vehicle_runtime.csv");
                break;
            case IntakeAirTemperature:
                System.out.println(dtf.format(LocalDateTime.now()));
                System.out.println("Ongoing Operation: OBD packages (Temperature). OBD_code: " + OBD_req);
                Vehicle_Operation = new File(FOLDERNAME + "/vehicle_Temp.csv");
                break;
            case ThrottlePosition:
                System.out.println(dtf.format(LocalDateTime.now()));
                System.out.println("Ongoing Operation: OBD packages (Throttle Position). OBD_code: " + OBD_req);
                Vehicle_Operation = new File(FOLDERNAME + "/vehicle_throttle.csv");
                break;
            case EngineRPM:
                System.out.println(dtf.format(LocalDateTime.now()));
                System.out.println("Ongoing Operation: OBD packages (Engine RPM). OBD_code: " + OBD_req);
                Vehicle_Operation = new File(FOLDERNAME + "/vehicle_RPM.csv");
                break;
            case VehicleSpeed:
                System.out.println(dtf.format(LocalDateTime.now()));
                System.out.println("Ongoing Operation: OBD packages (Vehicle Speed). OBD_code: " + OBD_req);
                Vehicle_Operation = new File(FOLDERNAME + "/vehicle_speed.csv");
                break;
            case CoolantTemperature:
                System.out.println(dtf.format(LocalDateTime.now()));
                System.out.println("Ongoing Operation: OBD packages (Temperature). OBD_code: " + OBD_req);
                Vehicle_Operation = new File(FOLDERNAME + "/vehicle_Temp.csv");
                break;
        }
        FileOutputStream vehicle_stream = new FileOutputStream(Vehicle_Operation);


        InetAddress hostAddress = InetAddress.getByName("155.207.18.208");
        DatagramSocket s = new DatagramSocket();
        byte[] txbuffer = OBD_req.getBytes();
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);
        DatagramSocket r = new DatagramSocket(clientPort);
        r.setSoTimeout(8000);
        byte[] rxbuffer = new byte[2048];

        ArrayList<Double> OBD_packages = new ArrayList<>();
        String nibble1, nibble2, Operation_Values = "";
        int valueXX = 0, valueYY = 0;
        long BeginTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - BeginTime < 240000) {
            s.send(p);
            DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);
            try {
                r.receive(q);
                int curr_len = q.getLength();
                byte[] curr_data = q.getData();
                if (curr_len == 11) {                   //Both XX and YY
                    nibble1 = "" + (char) curr_data[6] + (char) curr_data[7];
                    nibble2 = "" + (char) curr_data[9] + (char) curr_data[10];

                    valueXX = Integer.parseInt(nibble1, 16);
                    valueYY = Integer.parseInt(nibble2, 16);
                }else if (curr_len == 8){                //Only XX
                    nibble1 = "" + (char) curr_data[6] + (char) curr_data[7];

                    valueXX = Integer.parseInt(nibble1, 16);
                }else {
                    System.err.println("Unexpected input from OBD.");
                    exit(-1);
                }
                switch (Oper){
                    case EngineRunTime:
                        OBD_packages.add((double) (256 * valueXX + valueYY));
                        break;
                    case IntakeAirTemperature:
                        OBD_packages.add((double) (valueXX - 40));
                        break;
                    case ThrottlePosition:
                        OBD_packages.add((double) (valueXX * 100 / 255));
                        break;
                    case EngineRPM:
                        OBD_packages.add((double) (( (valueXX * 256) + valueYY) / 4));
                        break;
                    case VehicleSpeed:
                        OBD_packages.add((double) valueXX);
                        break;
                    case CoolantTemperature:
                        OBD_packages.add((double) (valueXX - 40));
                        break;
                }

            } catch (Exception x) {
                System.err.println("(OBD) packages didn't arrive.");
            }
        }
        for (Double obd_package : OBD_packages) {
            Operation_Values += obd_package + ",";
        }
        // Write Results to Files //
        try {
            vehicle_stream.write(Operation_Values.getBytes());
            vehicle_stream.close();
        } catch (IOException x) {
            System.out.println("Failure, writing results of Vehicle Operation.");
        }
        r.close();
    }

    private static void ParseInput()throws  IOException, LineUnavailableException{
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        Scanner scanner = new Scanner( System.in );
        System.out.println( "Type Echo for Echo Packages." );
        System.out.println( "Type Image for Image Receiving." );
        System.out.println( "Type Audio for Audio Receving." );
        System.out.println( "Type Copter for Ithaki Copter measurements." );
        System.out.println( "Type Vehicle for OBD-II Diagnostics." );
        System.out.println( "Type exit to stop the program." );
        System.out.println( " " );

        String input = scanner.nextLine();
        switch (input.toLowerCase()) {
            case "echo":
                Scanner echo_scanner = new Scanner( System.in );
                System.out.println( "Type ON (With Temperature) or OFF (Without Temperature)." );
                String echo_input = echo_scanner.nextLine();
                if ("on".equalsIgnoreCase(echo_input)){
                    EchoPackages(TemperatureON);
                }else if ("off".equalsIgnoreCase(echo_input)) {
                    EchoPackages(TemperatureOFF);
                }else{
                    System.err.println("Please, select a valid operation.");
                    exit(-1);
                }
                break;
            case "image":
                Scanner image_scanner = new Scanner( System.in );
                System.out.println( "Type FIX or PTZ." );
                String image_input = image_scanner.nextLine();
                if ("fix".equalsIgnoreCase(image_input)){
                    ImageRequest(CAMFIX);
                }else if ("ptz".equalsIgnoreCase(image_input)) {
                    ImageRequest(CAMPTZ);
                }else{
                    System.err.println("Please, select a valid image type.");
                    exit(-1);
                }
                break;
            case "audio":
                Scanner audio_scanner = new Scanner( System.in );
                System.out.println( "Type NON (for DPCM) or AQ (for AQ-DPCM)." );
                String audio_input = audio_scanner.nextLine();
                if ("non".equalsIgnoreCase(audio_input)){
                    System.out.println( "Type T (for frequency generator) or F (for music repertorium)." );
                    audio_input = audio_scanner.nextLine();
                    if ("F".equalsIgnoreCase(audio_input))
                        SoundRequestNonAdaptive(8, "F", null);
                    else if ("T".equalsIgnoreCase(audio_input))
                        SoundRequestNonAdaptive(8, "T", null);
                    else {
                        System.err.println("Please select a valid operation.");
                        exit(-1);
                    }
                }else if ("aq".equalsIgnoreCase(audio_input)) {
                    SoundRequestAdaptive(16, "F", "AQ");
                }else{
                    System.err.println("Please, select a valid audio type.");
                    exit(-1);
                }
                break;
            case "copter":
                Scanner copter_scanner = new Scanner( System.in );
                System.out.println( "Type the desired flightlevel." );
                String copter_input = copter_scanner.nextLine();
                IthakiCopter(Integer.parseInt(copter_input));
                break;
            case "vehicle":
                Scanner vehicle_scanner = new Scanner( System.in );
                System.out.println( "Type Runtime for Engine RunTime." );
                System.out.println( "Type Intake for Intake Air Temperature." );
                System.out.println( "Type Throttle for Throttle Position." );
                System.out.println( "Type Rpm for Engine Rpm." );
                System.out.println( "Type Speed for Vehicle Speed." );
                System.out.println( "Type Coolant for Coolant Temperature." );
                System.out.println( " " );
                String vehicle_input = vehicle_scanner.nextLine();
                if ("runtime".equalsIgnoreCase(vehicle_input)){
                    Onboard_Diagnostics(EngineRunTime);
                }else if ("intake".equalsIgnoreCase(vehicle_input)) {
                    Onboard_Diagnostics(IntakeAirTemperature);
                }else if ("throttle".equalsIgnoreCase(vehicle_input)){
                    Onboard_Diagnostics(ThrottlePosition);
                }else if ("rpm".equalsIgnoreCase(vehicle_input)) {
                    Onboard_Diagnostics(EngineRPM);
                }else if ("speed".equalsIgnoreCase(vehicle_input)){
                    Onboard_Diagnostics(VehicleSpeed);
                }else if ("coolant".equalsIgnoreCase(vehicle_input)) {
                    Onboard_Diagnostics(IntakeAirTemperature);
                }else{
                    System.err.println("Please, select a valid vehicle Operation.");
                    exit(-1);
                }
                break;
            case "exit":
                System.out.println("The Session has ended at " + dtf.format(LocalDateTime.now()));
                exit(1);
            default:
                System.err.println("Please type a valid Operation");
                exit(-1);

        }
    }

    private static byte[] convertBytes(ArrayList<Byte> bytes) {
        byte[] ret = new byte[bytes.size()];
        Iterator<Byte> iterator = bytes.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next().byteValue();
        }
        return ret;
    }

    private static boolean CheckAudioInput(int Q, String Y, String Encoding) {
        boolean flag = true;
        if ("AQ".equalsIgnoreCase(Encoding)) {
            if (!"F".equalsIgnoreCase(Y)) {
                System.err.println("The adaptive encoding must be used only at music repertoire.");
                flag = false;
            }
            if (Q != 16) {
                System.err.println("The number of bits, of the quantizer must be 16.");
                flag = false;
            }

        } else if (Encoding == null) {
            if (!"F".equalsIgnoreCase(Y) && !"T".equalsIgnoreCase(Y)) {
                System.err.println("Y must be, either T for sound generator or F for music repertoire.");
                flag = false;
            }
            if (Q != 8 && Q != 16) {
                System.err.println("The number of bits, of the quantizer must be 8 or 16.");
                flag = false;
            }

        } else {
            System.err.println("Encoding must be either null for NON-adaptive or AQ for adaptive.");
            flag = false;
        }

        return flag;
    }

}

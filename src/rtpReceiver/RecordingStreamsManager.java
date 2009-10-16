package rtpReceiver;

import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import com.memetic.media.rtp.RTPSocketAdapter;

import memetic.crypto.RTPCrypt;

import rtspd.LocalSession;
import rtspd.RTPListener;
import rtspd.RecordArchiveManager;
import rtspd.RecordNetworkTransport;

public class RecordingStreamsManager {

    private RecordArchiveManager recordArchiveManager=null;
    private RTPListener rtpListener=null;
    private RecordNetworkTransport rnt;




    public RecordingStreamsManager(String directory,  RTPSocketAdapter socket) {
        LocalSession session;
        try {
            session = new LocalSession(new URL("http",socket.getAddress().getHostName(),
                    socket.getAddress().getPort(),"").toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        recordArchiveManager = new RecordArchiveManager(directory,session,"local","");
        recordArchiveManager.enableRecording();
        socket.setRtpPacketSink(recordArchiveManager);
        socket.setRtcpPacketSink(recordArchiveManager);
    }
    
    public RecordingStreamsManager(String directory, RecordNetworkTransport rnt) {
        LocalSession session;
        this.rnt=rnt;
        System.out.println("RecordingStreamsManager " +this.rnt.getRtpSocket());
        try {
            session = new LocalSession(new URL("http",rnt.getEndpoint().getHost(),
                    rnt.getEndpoint().getPort(),"").toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        recordArchiveManager = new RecordArchiveManager(directory,session,"local","");
        recordArchiveManager.enableRecording();

        //add forward for receiving the recorded material locally
        recordArchiveManager.addForward("127.0.0.1");


        DatagramSocket rtpSocket=this.rnt.getRtpSocket();
        try {
            System.out.println("getBroadcast " + rtpSocket.getBroadcast());
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("rtpSocket " + rtpSocket.getPort());
        rtpListener=new RTPListener(rtpSocket,recordArchiveManager);
        System.out.println("rtpListener " + rtpListener.toString());
    }

    public void setDirectory(String directory){
        recordArchiveManager.changeSessionDir(directory);
    }

    public void startRecording(){
//        System.out.println("start recording");
//        System.out.println("rtpListener " + rtpListener.toString());
//        rtcpListener.record();
//        rtpListener.record();
        //rtcpListener.record();
    }

    public void stopRecording(){
        System.out.println("stop recording");
//        rtcpListener.stop();
//        rtpListener.stop();
        recordArchiveManager.disableRecording();
        recordArchiveManager.terminate();
    }

    public void setEncryption(RTPCrypt crypt){
        rnt.getEndpoint().setEncryption(crypt.getAlgorithmName(), crypt.getKey());
    }

    public long getRecordStart() {
        return recordArchiveManager.getRecordStart();
    }

    public long getRecordStop() {
        return recordArchiveManager.getRecordStop();
    }
}

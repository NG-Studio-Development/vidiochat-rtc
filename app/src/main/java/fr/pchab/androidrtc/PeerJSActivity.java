package fr.pchab.androidrtc;

import android.app.Activity;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PeerJSActivity extends Activity {

    private final static String TAG = "PEER_JS_ACTIVITY_LOG";

    private TextView tvIdUser;

    private static boolean factoryStaticInitialized;
    private GLSurfaceView surfaceView;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private VideoRenderer localRenderer;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private MediaStream localMediaStream;
    private boolean videoSourceStopped;
    private boolean initiator = true;
    private boolean video = true;
    private boolean audio = true;

    private WebSocketClient client;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private final PCObserver pcObserver = new PCObserver();
    private final SDPObserver sdpObserver = new SDPObserver();
    private MediaConstraints sdpMediaConstraints;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
    private LinkedList<IceCandidate> queuedRemoteCandidates = new LinkedList<IceCandidate>();
    private AudioManager audioManager;

    private Toast logToast;
    private final Boolean[] quit = new Boolean[] { false };
    private String id;

    private String token = "frpchabandroidrtcX"; // ����� ��������� ����� ��������, � ��������� ��� ������ ��� �����
    private String connectionId = "mc_frpchabandroidrtcX"; // ����� ��������� ����� ��������, ������ � ������ "mc_"

    private String roomKey = "thnr9tphiwdeipb9";
    private String friendId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_js);

        tvIdUser = (TextView) findViewById(R.id.idUser);

        Button buttonCall = (Button) findViewById(R.id.buttonCall);
        buttonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (client == null) {
                    createPC();
                } else {

                    //client.send("BUY");
                    disconnectAndExit();
                }
            }
        });

        surfaceView = new GLSurfaceView(this);
        surfaceView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        LinearLayout content = (LinearLayout)findViewById(R.id.activity_webrtc_content);
        content.addView(surfaceView);

        VideoRendererGui.setView(surfaceView, new Runnable() {
            @Override
            public void run() {

            }
        });
        remoteRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_FILL, false);
        localRender = VideoRendererGui.create(1, 74, 25, 25, VideoRendererGui.ScalingType.SCALE_FILL, false);

        if (!factoryStaticInitialized) {
            PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true, new Object());
            factoryStaticInitialized = true;
        }

        audioManager = ((AudioManager) getSystemService(AUDIO_SERVICE));




        @SuppressWarnings("deprecation")
        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        audioManager.setMode(isWiredHeadsetOn ? AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);

        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        /*iceServers.add(new PeerConnection.IceServer("stun:stun01.sipphone.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.ekiga.net"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.fwdnet.net"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.ideasip.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.ideasip.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.iptel.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.rixtelecom.se"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.schlund.de"));*/
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        /*iceServers.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stunserver.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.softjoys.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voiparound.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voipbuster.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voipstunt.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voxgratia.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.xten.com"));

        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        iceServers.add(new PeerConnection.IceServer("turn:homeo@turn.bistri.com:80","","homeo"));

        iceServers.add(new PeerConnection.IceServer("turn:numb.viagenie.ca","webrtc@live.com","muazkh"));
        iceServers.add(new PeerConnection.IceServer("turn:192.158.29.39:3478?transport=udp","28224511:1379330808","JZEOEt2V3Qb0y27GRntt2u2PAYA="));
        iceServers.add(new PeerConnection.IceServer("turn:192.158.29.39:3478?transport=tcp","28224511:1379330808","JZEOEt2V3Qb0y27GRntt2u2PAYA="));*/









    }



    void createPC(){
        quit[0] = false;

        factory = new PeerConnectionFactory();

        MediaConstraints pcConstraints = new MediaConstraints();
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        peerConnection = factory.createPeerConnection(iceServers, pcConstraints, pcObserver);
        // � ��� � ���� ���� �����������

        createDataChannelToRegressionTestBug2302(peerConnection);
        // �������� �����-�� �������� �����������

        logAndToast("Creating local video source...");
        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "240"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "320"));
        // ����� � �� ��������� ������ �����

        localMediaStream = factory.createLocalMediaStream("ARDAMS");
        VideoCapturer capturer = getVideoCapturer();
        videoSource = factory.createVideoSource(capturer, videoConstraints);
        videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
        localRenderer = new VideoRenderer(localRender);
        videoTrack.addRenderer(localRenderer); // ���� �����, ������� ����� ����� ���������
        localMediaStream.addTrack(videoTrack);

        audioTrack = factory.createAudioTrack("ARDAMSa0", factory.createAudioSource(new MediaConstraints())); // ���� ����� � ���������
        localMediaStream.addTrack(audioTrack);
        peerConnection.addStream(localMediaStream/*, new MediaConstraints()*/);

        getID();
        /*GetID getId = new GetID();
        try {
            getId.execute(); //��������� ����������� ������, ������� �������� http-������ �� ��������� id �� �������
        }catch (Exception e) {
            logAndToast("No Internet connection");
            disconnectAndExit();
        } */
    }


    private void getID() {
        String url = "http://0.peerjs.com:9000/" + roomKey + "/id?ts=" + Calendar.getInstance().getTimeInMillis() + ".7330598266421392";

        final RequestQueue queue = Volley.newRequestQueue(PeerJSActivity.this);

        final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        tvIdUser.setText(response);
                        onResponseGetID(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        StringRequest operatorIdRequest = new StringRequest(Request.Method.GET, "http://akimovdev.temp.swtest.ru/webrtc/handleIdOperator.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        friendId = response.replace("\r\n", "");
                        queue.add(stringRequest);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });


        queue.add(operatorIdRequest);
    }


    private void onResponseGetID(String result) {
        if (result==null)
            return;
        id = result.replace("\n", "");
        // ������� ���������, ������� ����� ����������� ���������� ������� ��� ������

        URI uri = null;
        try {
            // �������� URI ��� ������. ��� ������� peerjs �� ������ ����� ����� ���
            //uri = new URI("ws", "", "0.peerjs.com", 9000, "/peerjs", "key=" + roomKey + "&id=" + id + "&token=" + token, "");
            uri = new URI("ws", "", "0.peerjs.com", 9000, "/peerjs", "key=" + roomKey + "&id=" + id + "&token=" + token, "");
            // roomKey - ��� ��������, ��������� ��� ��
            // id - ������ ��� ���������� �� ������� id
            // token - ��������� ����� �������� (� ��������� ��� ������ ��� �����)
        } catch (URISyntaxException e) {
            disconnectAndExit();
        }

        client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (initiator){
                            logAndToast("Creating offer...");
                            peerConnection.createOffer(sdpObserver, sdpMediaConstraints);
                        }
                    }
                });
            }

            @Override
            public void onMessage(final String data) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            JSONObject json = new JSONObject(data);
                            String type = (String) json.get("type");
                            if (type.equalsIgnoreCase("candidate")) {
                                JSONObject jsonCandidate = json.getJSONObject("payload").getJSONObject("candidate");
                                IceCandidate candidate = new IceCandidate(
                                        (String) jsonCandidate.get("sdpMid"),
                                        jsonCandidate.getInt("sdpMLineIndex"),
                                        (String) jsonCandidate.get("candidate"));
                                if (queuedRemoteCandidates != null) {
                                    queuedRemoteCandidates.add(candidate);
                                } else {
                                    peerConnection.addIceCandidate(candidate);
                                }
                            } else if (type.equalsIgnoreCase("answer") || type.equalsIgnoreCase("offer")) {
                                connectionId = json.getJSONObject("payload").getString("connectionId");
                                friendId = json.getString("src");
                                JSONObject jsonSdp = json.getJSONObject("payload").getJSONObject("sdp");
                                SessionDescription sdp = new SessionDescription(
                                        SessionDescription.Type.fromCanonicalForm(type),
                                        preferISAC((String) jsonSdp.get("sdp")));
                                peerConnection.setRemoteDescription(sdpObserver, sdp);
                            } else if (type.equalsIgnoreCase("bye")) {
                                logAndToast("Remote end hung up; dropping PeerConnection");
                                disconnectAndExit();
                            } else {
                                //throw new RuntimeException("Unexpected message: " + data);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            //throw new RuntimeException(e);
                        }
                    }
                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        disconnectAndExit();
                    }
                });
            }

            @Override
            public void onError(Exception ex) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        disconnectAndExit();
                    }
                });
            }
        };

        client.connect();

    }

    // ����������� ��� ������� � �������
    private void disconnectAndExit() {
        synchronized (quit[0]) {
            if (quit[0]) {
                return;
            }
            quit[0] = true;
            if (peerConnection != null) {
                peerConnection.dispose();
                peerConnection = null;
            }
            if (client != null) {
                client.send("{\"type\": \"bye\"}");
                client.close(); // client.disconnect();
                client = null;
            }
            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }
            if (factory != null) {
                factory.dispose();
                factory = null;
            }
            //if (audioManager!=null)
                //audioManager.abandonAudioFocus(audioFocusListener);

            //finish();
        }
    }

    @Override
    public void onStop() {
        disconnectAndExit();
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        surfaceView.onPause();
        if (videoSource != null) {
            videoSource.stop(); // ������������� ���������� �����
            videoSourceStopped = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        surfaceView.onResume();
        if (videoSource != null && videoSourceStopped) {
            videoSource.restart(); // ������������ ���������� �����
        }
    }

    // �������� �� �����-�� ������
    private static void createDataChannelToRegressionTestBug2302(PeerConnection pc) {
        DataChannel dc = pc.createDataChannel("dcLabel", new DataChannel.Init());
        dc.close();
        dc.dispose();
    }

    //=
    private class PCObserver implements PeerConnection.Observer {
        @Override
        public void onIceCandidate(final IceCandidate candidate){
            runOnUiThread(new Runnable() {
                public void run() {
                    JSONObject json = new JSONObject();
                    JSONObject payload = new JSONObject();
                    JSONObject jsonCandidate = new JSONObject();
                    jsonPut(json, "type", "CANDIDATE");
                    jsonPut(jsonCandidate, "sdpMid", candidate.sdpMid);
                    jsonPut(jsonCandidate, "sdpMLineIndex", candidate.sdpMLineIndex);
                    jsonPut(jsonCandidate, "candidate", candidate.sdp);
                    jsonPut(payload, "candidate", jsonCandidate);
                    jsonPut(payload, "type", "media");
                    jsonPut(payload, "connectionId", connectionId);
                    jsonPut(json, "payload", payload);
                    jsonPut(json, "dst", friendId);
                    jsonPut(json, "src", id);
                    sendMessage(json);
                }
            });
        }

        /*@Override
        public void onError(){
            runOnUiThread(new Runnable() {
                public void run() {
                    disconnectAndExit();
                }
            });
        }*/

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {

        }

        @Override
        public void onAddStream(final MediaStream stream){
            runOnUiThread(new Runnable() {
                public void run() {
                    if (stream.videoTracks.size() == 1) {
                        stream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                    }
                }
            });
        }

        @Override
        public void onRemoveStream(final MediaStream stream){
            runOnUiThread(new Runnable() {
                public void run() {
                    stream.videoTracks.get(0).dispose();
                }
            });
        }

        @Override
        public void onDataChannel(final DataChannel dc) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }
    }
    //=

    private class SDPObserver implements SdpObserver {
        private SessionDescription localSdp;

        @Override
        public void onCreateSuccess(final SessionDescription origSdp) {
            final SessionDescription sdp = new SessionDescription(origSdp.type, preferISAC(origSdp.description));
            localSdp = sdp;
            runOnUiThread(new Runnable() {
                public void run() {
                    peerConnection.setLocalDescription(sdpObserver, sdp);
                }
            });
        }

        private void sendLocalDescription() {
            logAndToast("Sending " + localSdp.type);
            JSONObject json = new JSONObject();
            JSONObject payload = new JSONObject();
            JSONObject sdp = new JSONObject();
            jsonPut(json, "type", localSdp.type.canonicalForm().toUpperCase());
            jsonPut(sdp, "sdp", localSdp.description);
            jsonPut(sdp, "type", localSdp.type.canonicalForm().toLowerCase());
            jsonPut(payload, "sdp", sdp);
            jsonPut(payload, "type", "media");
            jsonPut(payload, "connectionId", connectionId);
            jsonPut(payload, "browser", "Chrome");
            jsonPut(json, "payload", payload);
            jsonPut(json, "dst", friendId);
            sendMessage(json);
        }

        @Override
        public void onSetSuccess() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (initiator) {
                        if (peerConnection.getRemoteDescription() != null) {
                            drainRemoteCandidates();
                        } else {
                            sendLocalDescription();
                        }
                    } else {
                        if (peerConnection.getLocalDescription() == null) {
                            logAndToast("Creating answer");
                            peerConnection.createAnswer(SDPObserver.this, sdpMediaConstraints);
                        } else {
                            sendLocalDescription();
                            drainRemoteCandidates();
                        }
                    }
                }
            });
        }

        @Override
        public void onCreateFailure(final String error) {

        }

        @Override
        public void onSetFailure(final String error) {

        }

        private void drainRemoteCandidates() {
            for (IceCandidate candidate : queuedRemoteCandidates) {
                peerConnection.addIceCandidate(candidate);
            }
            //queuedRemoteCandidates = null;
            queuedRemoteCandidates.clear();
        }
    }



    // ����� �� �������� ��������� ����� � ������
    private VideoCapturer getVideoCapturer() {
        String[] cameraFacing = { "front", "back" };
        int[] cameraIndex = { 0, 1 };
        int[] cameraOrientation = { 0, 90, 180, 270 };
        for (String facing : cameraFacing) {
            for (int index : cameraIndex) {
                for (int orientation : cameraOrientation) {
                    String name = "Camera " + index + ", Facing " + facing + ", Orientation " + orientation;
                    VideoCapturer capturer = VideoCapturer.create(name);
                    if (capturer != null) {
                        logAndToast("Using camera: " + name);
                        return capturer;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        disconnectAndExit();
        super.onDestroy();
    }

    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void sendMessage(JSONObject json) {
        client.send(json.toString()); // ���������� ���������
    }

    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {

        }
    }

    // ������� �����, ������� � �� ������ ������. �� ��������� sdp-���������
    private static String preferISAC(String sdpDescription) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String isac16kRtpMap = null;
        Pattern isac16kPattern = Pattern.compile("^a=rtpmap:(\\d+) ISAC/16000[\r]?$");
        for (int i = 0; (i < lines.length) && (mLineIndex == -1 || isac16kRtpMap == null); ++i) {
            if (lines[i].startsWith("m=audio ")) {
                mLineIndex = i;
                continue;
            }
            Matcher isac16kMatcher = isac16kPattern.matcher(lines[i]);
            if (isac16kMatcher.matches()) {
                isac16kRtpMap = isac16kMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
            Log.d(TAG, "No m=audio line, so can't prefer iSAC");
            return sdpDescription;
        }
        if (isac16kRtpMap == null) {
            Log.d(TAG, "No ISAC/16000 line, so can't prefer iSAC");
            return sdpDescription;
        }
        String[] origMLineParts = lines[mLineIndex].split(" ");
        StringBuilder newMLine = new StringBuilder();
        int origPartIndex = 0;
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(isac16kRtpMap);
        for (; origPartIndex < origMLineParts.length; ++origPartIndex) {
            if (!origMLineParts[origPartIndex].equals(isac16kRtpMap)) {
                newMLine.append(" ").append(origMLineParts[origPartIndex]);
            }
        }
        lines[mLineIndex] = newMLine.toString();
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

}

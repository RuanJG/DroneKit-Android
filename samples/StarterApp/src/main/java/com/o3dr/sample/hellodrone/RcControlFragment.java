package com.o3dr.sample.hellodrone;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.o3dr.android.client.Drone;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RcControlFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RcControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RcControlFragment extends Fragment {
    private static final String TAG = "RcControlFragment";

    private JgRcOutput mRcOutput = null ;
    private Drone mDrone = null;
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            if( mRcOutput == null) {
                super.handleMessage(msg);
                return;
            }
            switch (msg.what) {
                case JgRcOutput.ALLID:
                    SeekBar thr = (SeekBar)getActivity().findViewById(R.id.thrBar);
                    if( thr!=null && mRcOutput!=null) thr.setProgress(mRcOutput.getRcById(JgRcOutput.THRID));
                    SeekBar yaw = (SeekBar)getActivity().findViewById(R.id.yawBar);
                    if( thr!=null && mRcOutput!=null) thr.setProgress(mRcOutput.getRcById(JgRcOutput.YAWID));
                    SeekBar roll = (SeekBar)getActivity().findViewById(R.id.rollBar);
                    if( thr!=null && mRcOutput!=null) thr.setProgress(mRcOutput.getRcById(JgRcOutput.ROLLID));
                    SeekBar pitch = (SeekBar)getActivity().findViewById(R.id.pitchBar);
                    if( thr!=null && mRcOutput!=null) thr.setProgress(mRcOutput.getRcById(JgRcOutput.PITCHID));
                    break;
                default:
                    alertUser("unknow msg frome rcoutput");
                    break;
            }
            super.handleMessage(msg);
        }
    };



    //*********************************** init get parameter
    public static RcControlFragment newInstance(String param1, String param2) {
        RcControlFragment fragment = new RcControlFragment();
        Bundle args = new Bundle();
        args.putString(TAG, param1);
        args.putString(TAG, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public RcControlFragment() {
        // Required empty public constructor
    }
    private String mParam1;
    private String mParam2;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(TAG);
            mParam2 = getArguments().getString(TAG);
        }
    }


    //**********************************  Fragment  Function
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rc_control, container, false);
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        alertUser("onAttach ");
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        alertUser("onDetach");
    }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSwitch = (Switch) getActivity().findViewById(R.id.RcSwitch);
        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSwitchClick();
            }
        });
    }




    //*************************************** ui event
    Switch mSwitch;
    private void onSwitchClick(){
        if( mSwitch.isChecked() ) {
            if (startRcLoop()) {
                alertUser("start Rc Running");
            } else {
                alertUser("Rc Start failed , Ensure Connected");
            }
        }else {
            stopRcLoop();
            alertUser("start Rc Stop");
        }
    }

    //**************************************** my listener

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
        public Drone getDone();
    }
    private OnFragmentInteractionListener mListener;
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }



    //************************************ drone function
    public boolean isRcRunning(){
        if(mRcOutput!=null && mRcOutput.isStarted())
            return true;
        else
            return false;
    }
    private boolean startRcLoop(){
        if( mListener == null && isRcRunning() ) {
            return false;
        }
        mDrone = mListener.getDone();
        if( mDrone == null || !mDrone.isConnected()) {
            return false;
        }

        mRcOutput = new JgRcOutput(mDrone,this.getActivity().getApplicationContext(),mHandler);
        mRcOutput.setmMode(JgRcOutput.SOFTWAREMODE);
        mRcOutput.setRate(50);//50ms on time
        if( mRcOutput.isReady() &&  mRcOutput.start() )
            return true;
        else{
            alertUser("start Rc Sending Failed");
            mRcOutput = null;
            return false;
        }
    }
    private boolean stopRcLoop(){
        if( isRcRunning() ){
            if( mRcOutput.stop() ) {
                mRcOutput = null;
                alertUser("Rc Sending Stoped");
                return true;
            }else{
                mRcOutput = null;
                return false;
            }
        }
        return true;
    }
    private boolean isReady() {
        if (mDrone != null && mRcOutput != null && mRcOutput.isReady()){
            return true;
        }else {
            return false;
        }
    }
    private int rcRange = 100;
    private boolean hasPressKey=false;
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        debugMsg("a key down:" + keyCode);
        if( !isRcRunning() )
            return true;
        int rc;
        switch (keyCode) {
            case KeyEvent.KEYCODE_W:
                //thr up
                mRcOutput.setRcById(JgRcOutput.THRID, (short) (mRcOutput.getRcById(JgRcOutput.THRID) + rcRange));
                break;
            case KeyEvent.KEYCODE_S:
                //thr down
                mRcOutput.setRcById(JgRcOutput.THRID, (short) (mRcOutput.getRcById(JgRcOutput.THRID) - rcRange));
                break;
            default:
                break;
        }
        if( !hasPressKey ) switch (keyCode) {
            case KeyEvent.KEYCODE_A:
                //yaw sub
                mRcOutput.setRcById(JgRcOutput.YAWID, (short) (mRcOutput.getRcById(JgRcOutput.YAWID) - rcRange));
                break;
            case KeyEvent.KEYCODE_D:
                //yaw add
                mRcOutput.setRcById(JgRcOutput.YAWID, (short) (mRcOutput.getRcById(JgRcOutput.YAWID) + rcRange));
                break;
            case KeyEvent.KEYCODE_4:
                //pitch add
                mRcOutput.setRcById(JgRcOutput.PITCHID, (short) (mRcOutput.getRcById(JgRcOutput.PITCHID) + rcRange));
                break;
            case KeyEvent.KEYCODE_2:
                //pitch sub
                mRcOutput.setRcById(JgRcOutput.PITCHID, (short) (mRcOutput.getRcById(JgRcOutput.PITCHID) - rcRange));
                break;
            case KeyEvent.KEYCODE_1:
                //roll sub
                mRcOutput.setRcById(JgRcOutput.ROLLID, (short) (mRcOutput.getRcById(JgRcOutput.ROLLID) - rcRange));
                break;
            case KeyEvent.KEYCODE_3:
                //roll add
                mRcOutput.setRcById(JgRcOutput.ROLLID, (short) (mRcOutput.getRcById(JgRcOutput.ROLLID) + rcRange));
                break;
            default:
                break;
        }
        hasPressKey = true;
        return true;
    }
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        debugMsg("a key up:" + keyCode);
        if( !isReady() )
            return true;

        switch (keyCode){
            case KeyEvent.KEYCODE_W:
                //thr up
                //mRcOutput.setRcById(JgRcOutput.THRID,(short)(mRcOutput.getRcById(JgRcOutput.THRID)-rcRange));
                break;
            case KeyEvent.KEYCODE_S:
                //thr down
                //mRcOutput.setRcById(JgRcOutput.THRID,(short)(mRcOutput.getRcById(JgRcOutput.THRID)+rcRange));
                break;
            case KeyEvent.KEYCODE_A:
                //yaw sub
                mRcOutput.setRcById(JgRcOutput.YAWID,(short)(mRcOutput.getRcById(JgRcOutput.YAWID)+rcRange));
                break;
            case KeyEvent.KEYCODE_D:
                //yaw add
                mRcOutput.setRcById(JgRcOutput.YAWID,(short)(mRcOutput.getRcById(JgRcOutput.YAWID)-rcRange));
                break;
            case KeyEvent.KEYCODE_4:
                //pitch add
                mRcOutput.setRcById(JgRcOutput.PITCHID,(short)(mRcOutput.getRcById(JgRcOutput.PITCHID)-rcRange));
                break;
            case KeyEvent.KEYCODE_2:
                //pitch sub
                mRcOutput.setRcById(JgRcOutput.PITCHID,(short)(mRcOutput.getRcById(JgRcOutput.PITCHID)+rcRange));
                break;
            case KeyEvent.KEYCODE_1:
                //roll sub
                mRcOutput.setRcById(JgRcOutput.ROLLID,(short)(mRcOutput.getRcById(JgRcOutput.ROLLID)+rcRange));
                break;
            case KeyEvent.KEYCODE_3:
                //roll add
                mRcOutput.setRcById(JgRcOutput.ROLLID,(short)(mRcOutput.getRcById(JgRcOutput.ROLLID)-rcRange));
                break;
            default:
                break;
        }

        hasPressKey = false;

        return true;
    }




    //********************************** debug function
    protected void alertUser(String message) {
        Toast.makeText(this.getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
        Log.d(TAG, message);
    }
    protected void debugMsg(String msg){
        Log.d(TAG, msg);
    }

}

package com.mahmoud_ashraf.phoneauthenticateex

import android.app.ProgressDialog
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_login.*
import android.widget.Toast
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.content.Intent
import android.os.CountDownTimer
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class LoginActivity : AppCompatActivity() , View.OnClickListener {

    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var phoneNumber: String? = null
    private var mVerificationId: String? = null
    private var mResendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var progressDialog: ProgressDialog? = null
    private var isTimerActive = false
    private var mCounterDown : CountDownTimer? = null
    private var timeLeft : Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if(savedInstanceState==null) {
            initView()
            startVerfiy()
        }
        else {
            phoneNumber = savedInstanceState.getString("phoneNumber")
            initView()
            startPhoneNumberVerification(phoneNumber.toString())


            timeLeft = savedInstanceState.getLong("timeLeft")
            showTimer(timeLeft)
        }
    }
    override fun onClick(v: View?) {
        when (v){
            verification_btn -> {
                // try to enter the code by yourself to handle the case
                // if user enter another sim card used in another phone ...
                    var code = sent_codeEt.text
                Log.e("code is --- ",code.toString())
                if(code!=null&&!code.isEmpty()&&mVerificationId!=null&&!mVerificationId!!.isEmpty()){

                    showProgressDialog(this, "please wait...", false)

                    val credential = PhoneAuthProvider.getCredential(mVerificationId!!, code.toString())
                    signInWithPhoneAuthCredential(credential)
                }
            }

            resend_btn -> {
                if(mResendToken!=null&&!isTimerActive) {
                    resendVerificationCode(phoneNumber.toString(),mResendToken)
                    showTimer(60000)
                    showProgressDialog(this, "Sending a verification code", false)
                }
                else {
                    toast("Sorry, You Can't request new code now, Please wait ...")
                }

            }

            wrong_tv -> {
               showLoginActivity()
            }
        }
    }
    private fun resendVerificationCode(phoneNumber: String, token: PhoneAuthProvider.ForceResendingToken?) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber, // Phone number to verify
            60, // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            this, // Activity (for callback binding)
            callbacks, // OnVerificationStateChangedCallbacks
            token) // ForceResendingToken from callbacks
    }
    private fun initView(){
        // init vars from bundle
        phoneNumber = intent.getStringExtra("phoneNumber")
        verfiy_Tv.setText("Verfiy $phoneNumber")
        waiting_tv.setText(phoneNumber)

        // init click listener
        verification_btn.setOnClickListener(this)
        resend_btn.setOnClickListener(this)
        wrong_tv.setOnClickListener(this)

        // init fire base verfiyPhone number callback
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                if (progressDialog != null) {
                    dismissProgressDialog(progressDialog)
                }
                notifyUserAndRetry("Time Out :( failed.Retry again!")
            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.e("onVerificationCompleted", "onVerificationCompleted:$credential")
                if (progressDialog != null) {
                    dismissProgressDialog(progressDialog)
                }

               val smsMessageSent : String = credential.smsCode.toString()
                Log.e("the message is ----- ",smsMessageSent)
                if(smsMessageSent!=null)
                sent_codeEt.setText(smsMessageSent)

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.e("+++2", "onVerificationFailed", e)

                if (progressDialog != null) {
                    dismissProgressDialog(progressDialog)
                }

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Log.e("Exception:", "FirebaseAuthInvalidCredentialsException",e)
                    Log.e("=========:", "FirebaseAuthInvalidCredentialsException "+e.message)


                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.e("Exception:", "FirebaseTooManyRequestsException",e)
                }

                // Show a message and update the UI
                notifyUserAndRetry("Your Phone Number might be wrong or connection error.Retry again!")

            }

            override fun onCodeSent(verificationId: String?, token: PhoneAuthProvider.ForceResendingToken) {
                //for low level version which doesn't do auto verification save the verification code and the token


                dismissProgressDialog(progressDialog)
                counter_downTv.visibility = View.GONE
                // Save verification ID and resending token so we can use them later
                Log.e("onCodeSent===", "onCodeSent:$verificationId")

                mVerificationId = verificationId
                mResendToken = token

            }
        }
    }
    private fun startVerfiy(){

        Log.e("User Phone Number ===  ", phoneNumber)

        if (phoneNumber != null && !phoneNumber!!.isEmpty()) {
            startPhoneNumberVerification(phoneNumber!!)
            showTimer(60000)
            showProgressDialog(this, "Sending a verification code", false)
        } else {
            toast("Please enter a valid number to continue!")
        }
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    if (progressDialog != null) {
                        dismissProgressDialog(progressDialog)
                    }

                    val user = task.result.user
                    Log.e("Sign in with phone auth", "Success $user")
                    showHomeActivity()
                } else {

                    if (progressDialog != null) {
                        dismissProgressDialog(progressDialog)
                    }

                    notifyUserAndRetry("Your Phone Number Verification is failed.Retry again!")
                }
            }
    }
    // This method will send a code to a given phone number as an SMS
    private fun startPhoneNumberVerification(phoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,      // Phone number to verify
            60,               // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            this,            // Activity (for callback binding)
            callbacks) // OnVerificationStateChangedCallbacks


    }
    private fun notifyUserAndRetry(message: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("Ok",
            DialogInterface.OnClickListener { arg0, arg1 -> showLoginActivity() })

        alertDialogBuilder.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialog, which -> showLoginActivity() })

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
    // helpers methods :)
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun showTimer(milliesInFuture :Long){
        isTimerActive = true
       mCounterDown = object : CountDownTimer(milliesInFuture, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                counter_downTv.visibility = View.VISIBLE
                counter_downTv.text = "seconds remaining: " + millisUntilFinished / 1000



                //here you can have your logic to set text to edittext
            }

            override fun onFinish() {
                counter_downTv.visibility  = View.GONE
                isTimerActive = false
            }

        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("timeLeft", timeLeft)
        outState.putString("phoneNumber", phoneNumber)


    }
    private fun showProgressDialog(mActivity: Context, message: String, isCancelable: Boolean): ProgressDialog {
        progressDialog = ProgressDialog(mActivity)
        progressDialog!!.show()
        progressDialog!!.setCancelable(isCancelable)
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.setMessage(message)
        return progressDialog as ProgressDialog
    }
    private fun dismissProgressDialog(progressDialog: ProgressDialog?) {
        if (progressDialog != null && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
    private fun showLoginActivity() {
        startActivity( Intent(this, MainActivity::class.java)
         .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
    private fun showHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()


    }
    override fun onBackPressed() {
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (mCounterDown != null) {
            mCounterDown!!.cancel()
        }
    }


}

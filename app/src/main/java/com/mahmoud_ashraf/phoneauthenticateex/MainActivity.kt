package com.mahmoud_ashraf.phoneauthenticateex

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var phoneNumber: String? = null
    private var countryCode: String? = null
    private var alertDialogBuilder :AlertDialog.Builder? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews(){
        next_btn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v){
            next_btn -> {
                countryCode = ccp!!.selectedCountryCodeWithPlus
                phoneNumber = phoneNumberEt.text.toString()
                phoneNumber = "" + countryCode + phoneNumber

                Log.e("User Phone Number ===  ", phoneNumber)

                if (validatePhoneNumber(phoneNumberEt.text.toString())) {
                    notifyUserBeforeVerify("We will be verfiying the phone number:\n\n$phoneNumber\n" +
                          "Is this OK, or would you like to edit the number? -_-")
                } else {
                    toast("Please enter a valid number to continue!")
                }

            }
        }

    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun validatePhoneNumber(phone :String): Boolean {
        if (TextUtils.isEmpty(phone)) {
            return false
        }

        return true
    }

    private fun notifyUserBeforeVerify(message: String) {
         alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder!!.setMessage(message)
        alertDialogBuilder!!.setPositiveButton("Ok") { _, _ ->
               showLoginActivity()
            }

        alertDialogBuilder!!.setNegativeButton("Edit") { dialog, _ ->
                dialog.dismiss()
            }

        alertDialogBuilder!!.setCancelable(false)
       alertDialogBuilder!!.create()
        alertDialogBuilder!!.show()
    }



    private fun showLoginActivity() {
        startActivity(
            Intent(this,LoginActivity::class.java).putExtra("phoneNumber",phoneNumber))
        // close the current ;)
        finish()
    }



}


package com.fazpass.otp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.chaos.view.PinView
import com.fazpass.otp.model.Response
import com.fazpass.otp.utils.Helper.Companion.makeLinks
import com.google.android.material.button.MaterialButton

internal class FazpassVerificationPage(onComplete:(Boolean)->Unit, otpResponse: Response) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
              dismiss()
            }
        }
    }

    val complete = onComplete
    private var response = otpResponse
    private lateinit var digitContainer: LinearLayout
    private lateinit var digit: EditText
    private lateinit var btnVerify: MaterialButton
    private lateinit var imgLogo: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvTarget: TextView
    private lateinit var tvDetail: TextView
    private lateinit var tvResend: TextView
    private lateinit var otp: PinView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        return inflater.inflate(R.layout.fazpass_verification, container, false)
    }
    override fun getTheme(): Int {
        return R.style.DialogTheme
    }
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var otpLength = 0
        try{
            otpLength = response.data?.otp_length.toString().toInt()
        }catch (e:java.lang.Exception){
        }

        digitContainer = view.findViewById(R.id.llValidationOtpDigit)
        imgLogo = view.findViewById(R.id.imgValidationLogo)
        tvTitle = view.findViewById(R.id.tvValidationTitle)
        tvTarget = view.findViewById(R.id.tvValidationMobile)
        tvDetail = view.findViewById(R.id.tvPleaseInsert)
        var x = ""
        for (index in 0 until otpLength) {
            x+="X"
        }
        if(response.data?.channel.toString().uppercase()=="SMS"){
            imgLogo.setImageResource(R.drawable.sms)
            tvTitle.setText(R.string.we_send_verification_code_to_your_sms)
            tvTarget.text = response.target?.dropLast(4).plus("XXXX")
            tvDetail.setText(R.string.please_insert_your_verification_code)
        }else if (response.data?.channel.toString().uppercase()=="MISSCALL"){
            imgLogo.setImageResource(R.drawable.call)
            tvTitle.setText(R.string.we_send_verification_code_as_a_missed_call)
            tvTarget.text = response.data?.prefix?.plus(x)
            tvDetail.text = "Please insert $otpLength digit of last number that missed call you"
        }else if (response.data?.channel.toString().uppercase()=="WHATSAPP"){
            imgLogo.setImageResource(R.drawable.whatsapp)
            tvTitle.setText(R.string.we_send_verification_code_to_your_whatsapp)
            tvTarget.text = response.target?.dropLast(4).plus("XXXX")
            tvDetail.setText(R.string.please_insert_your_verification_code)
        }else if (response.data?.channel.toString().uppercase()=="WA_LONG_NUMBER"){
            imgLogo.setImageResource(R.drawable.whatsapp)
            tvTitle.setText(R.string.we_send_verification_code_to_your_whatsapp)
            tvTarget.text = response.target?.dropLast(4).plus("XXXX")
            tvDetail.setText(R.string.please_insert_your_verification_code)
        }else if (response.data?.channel.toString().uppercase()=="EMAIL"){
            imgLogo.setImageResource(R.drawable.email)
            tvTitle.setText(R.string.we_send_verification_code_to_your_email)
            tvTarget.text = response.target?.replaceRange(3,8,"xxxxx")
            tvDetail.setText(R.string.please_insert_your_verification_code)
        }

        val pinView:PinView = view.findViewById(R.id.otpPin)
        pinView.itemCount= otpLength

        btnVerify = view.findViewById(R.id.button)
        btnVerify.setOnClickListener {
            val otp = pinView.text.toString()
            if(otp.length==otpLength){
                verify(otp, view.context)
            }else{
                Toast.makeText(view.context,"OTP has not been filled completely",Toast.LENGTH_LONG).show()
            }

        }

        tvResend = view.findViewById(R.id.tvResend)
        tvResend.setText(R.string.not_received_the_code_resend_here)
        tvResend.makeLinks( Pair("here", View.OnClickListener {
            if(otpLength<=0){
                dismiss()
            }else{
                Loading.showDialog(view.context, false)
                val m = Merchant()
                when (response.target) {
                    "generate" -> {
                        response.target?.let { target -> m.generateOtp(target) { it->
                            response = it
                            Loading.hideLoading()
                        } }
                    }
                    "send" -> {
                        response.target?.let { target ->
                            response.data?.otp?.let { otp ->
                                m.sendOtp(target, otp) { it->
                                    response = it
                                    Loading.hideLoading()
                                }
                            }
                        }
                    }
                    "request" -> {
                        response.target?.let { target -> m.requestOtp(target) { it->
                            response = it
                            Loading.hideLoading()
                        } }
                    }
                }

            }

        }))

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun verify(otp: String, context: Context){
        removeKeyboard()
        if(Fazpass.isOnline(context)){
            Loading.showDialog(context, false)
            val m = Merchant()
            val otpId = response.data?.id
            if (otpId != null) {
                m.verifyOtp(otpId, otp){
                    Loading.hideLoading()
                    complete(it)
                }
            }else{
                Loading.hideLoading()
                complete(false)
            }
        }else{
            Toast.makeText(context,"Verification failed cause you are in offline mode", Toast.LENGTH_LONG).show()
        }

    }

    private fun removeKeyboard(){
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = requireActivity().currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }



}

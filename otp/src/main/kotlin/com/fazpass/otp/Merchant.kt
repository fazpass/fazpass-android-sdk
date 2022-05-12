package com.fazpass.otp

import android.util.Log
import com.fazpass.otp.model.GenerateOtpRequest
import com.fazpass.otp.model.GenerateOtpResponse
import com.fazpass.otp.usecase.MerchantUseCase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Anvarisy on 5/11/2022.
 * fazpass
 * anvarisy@fazpass.com
 */
open class Merchant {
    internal lateinit var merchantKey:String
    private lateinit var gatewayKey:String

    fun setGateway(gatewayKey:String){
        this.gatewayKey = gatewayKey
    }

    fun generateOtp(phone: String, gatewayKey:String,onComplete:(GenerateOtpResponse)->Unit){
        startGenerate(phone, gatewayKey,onComplete)
    }

    fun generateOtp(phone:String, onComplete:(GenerateOtpResponse)->Unit){
       startGenerate(phone,this.gatewayKey,onComplete)
    }

    private fun startGenerate(phone: String, gatewayKey: String, onComplete:(GenerateOtpResponse)->Unit){
        val fazpass by lazy { MerchantUseCase.start() }
        var response = GenerateOtpResponse(false,"",null, phone,null)
        fazpass.generateOtp("Bearer $merchantKey",GenerateOtpRequest( gatewayKey, phone)).
        subscribeOn(Schedulers.io()).
        observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    response = result
                    response.phone = phone
                    onComplete(response)
                },
                { error ->
                    response.error = error.message
                    onComplete(response)
                }
            )

    }

/*    open fun setOnGenerateOtp(l: OnGenerateOtp) {
        throw RuntimeException("Stub!")
    }

    interface OnGenerateOtp{
        fun success(response: GenerateOtpResponse){

        }

        fun error(message: String){

        }
    }*/
}
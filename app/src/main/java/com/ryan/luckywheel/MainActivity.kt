package com.ryan.luckywheel

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast

import java.util.ArrayList
import java.util.Random

import rubikstudio.library.LuckyWheelView
import rubikstudio.library.model.LuckyItem

class MainActivity : Activity(), LuckyWheelView.PostSpinListener {

    internal var data: MutableList<LuckyItem> = ArrayList()

    private val randomIndex: Int
        get() {
            val rand = Random()
            return rand.nextInt(data.size - 1) + 0
        }

    private val randomRound: Int
        get() {
            val rand = Random()
            return rand.nextInt(10) + 15
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val luckyWheelView = findViewById<LuckyWheelView>(R.id.luckyWheel)

        val luckyItem1 = LuckyItem()
        luckyItem1.topText = "COINS"
        luckyItem1.secondaryText = "100"
        //luckyItem1.icon = R.drawable.test1
        luckyItem1.color = -0xc20
        data.add(luckyItem1)

        val luckyItem2 = LuckyItem()
        luckyItem2.topText = "COINS"
        luckyItem2.secondaryText = "$5"
        //luckyItem2.icon = R.drawable.test2
        luckyItem2.color = -0x1f4e
        data.add(luckyItem2)

        val luckyItem3 = LuckyItem()
        luckyItem3.topText = "COINS"
        luckyItem3.secondaryText = "300"
        //luckyItem3.icon = R.drawable.test3
        luckyItem3.color = -0x3380
        data.add(luckyItem3)

        //////////////////
        val luckyItem4 = LuckyItem()
        luckyItem4.topText = "GIFT CARD"
        luckyItem4.secondaryText = "$5"
        //luckyItem4.icon = R.drawable.test4
        luckyItem4.color = -0xc20
        data.add(luckyItem4)

        val luckyItem5 = LuckyItem()
        luckyItem5.topText = "COINS"
        luckyItem5.secondaryText = "500"
        //luckyItem5.icon = R.drawable.test5
        luckyItem5.color = -0x1f4e
        data.add(luckyItem5)

        val luckyItem6 = LuckyItem()
        luckyItem6.topText = "COINS"
        luckyItem6.secondaryText = "$5"
        //luckyItem6.icon = R.drawable.test6
        luckyItem6.color = -0x3380
        data.add(luckyItem6)
        //////////////////

        //////////////////////
        val luckyItem7 = LuckyItem()
        luckyItem7.topText = "COINS"
        luckyItem7.secondaryText = "700"
        //luckyItem7.icon = R.drawable.test7
        luckyItem7.color = -0xc20
        data.add(luckyItem7)

        val luckyItem8 = LuckyItem()
        luckyItem8.topText = "GIFT CARD"
        luckyItem8.secondaryText = "$5"
        //luckyItem8.icon = R.drawable.test8
        luckyItem8.color = -0x1f4e
        data.add(luckyItem8)


        val luckyItem9 = LuckyItem()
        luckyItem9.topText = "COINS"
        luckyItem9.secondaryText = "900"
        //luckyItem9.icon = R.drawable.test9
        luckyItem9.color = -0x3380
        data.add(luckyItem9)
        ////////////////////////

        val luckyItem10 = LuckyItem()
        luckyItem10.topText = "COINS"
        luckyItem10.secondaryText = "$5"
        //luckyItem10.icon = R.drawable.test10
        luckyItem10.color = -0x1f4e
        data.add(luckyItem10)

        val luckyItem11 = LuckyItem()
        luckyItem11.topText = "GIFT CARD"
        luckyItem11.secondaryText = "2000"
        //luckyItem11.icon = R.drawable.test10
        luckyItem11.color = -0x1f4e
        data.add(luckyItem11)

        val luckyItem12 = LuckyItem()
        luckyItem12.topText = "GIFT CARD"
        luckyItem12.secondaryText = "3000"
        //luckyItem12.icon = R.drawable.test10
        luckyItem12.color = -0x1f4e
        data.add(luckyItem12)

        val luckyItem13 = LuckyItem()
        luckyItem13.topText = "GIFT CARD"
        luckyItem13.secondaryText = "$5"
        //luckyItem12.icon = R.drawable.test10
        luckyItem13.color = -0x1f4e
        data.add(luckyItem13)

        val luckyItem14 = LuckyItem()
        luckyItem14.topText = "GIFT CARD"
        luckyItem14.secondaryText = "3000"
        //luckyItem12.icon = R.drawable.test10
        luckyItem14.color = -0x1f4e
        data.add(luckyItem14)

        val luckyItem15 = LuckyItem()
        luckyItem15.topText = "GIFT CARD"
        luckyItem15.secondaryText = "$5"
        //luckyItem12.icon = R.drawable.test10
        luckyItem15.color = -0x1f4e
        data.add(luckyItem15)

        val luckyItem16 = LuckyItem()
        luckyItem16.topText = "GIFT CARD"
        luckyItem16.secondaryText = "3000"
        //luckyItem12.icon = R.drawable.test10
        luckyItem16.color = -0x1f4e
        data.add(luckyItem16)

        /////////////////////

        luckyWheelView.setPostSpinListener(this)
        luckyWheelView.setData(data)

        findViewById<View>(R.id.play).setOnClickListener {
            val index = randomIndex
            //luckyWheelView.startLuckyWheelWithTargetIndex(index);
        }

        luckyWheelView.setLuckyRoundItemSelectedListener(object : LuckyWheelView.LuckyRoundItemSelectedListener {
            override fun luckyRoundItemSelected(index: Int) {
                Toast.makeText(applicationContext, data[index].secondaryText, Toast.LENGTH_SHORT).show()
                Log.d("antonhttp", "YOU WON: " + data[index].secondaryText)
            }

            override fun onLuckyWheelRotationStart() {
                luckyWheelView.setInitialAngle(30f)
                Toast.makeText(applicationContext, "onstart initial angle 0", Toast.LENGTH_SHORT).show()
            }
        })

        val targetAngle: Float = (((360f * 3) + 270f - getAngleOfIndexTarget(getInitialSelectedIndex()) - (360f / data.size) / 2)) + -90
        luckyWheelView.setInitialAngle(targetAngle)

    }

    fun getInitialSelectedIndex(): Int {
        try {
            return data.indexOf(data.maxBy { it.secondaryText.toInt() })
        } catch (nfe: java.lang.NumberFormatException) {
            return data.indexOf(data.maxBy { it.secondaryText })
        }
    }

    fun getAngleOfIndexTarget(index: Int): Float {
        return (360f / data.size) * index
    }

    override fun onPostSpinComplete() {
        Log.d("antonhttp", "SCALE IS TRIGGERED")
    }

    override fun onPostSpinStart() {
        //to be implemented
    }

}

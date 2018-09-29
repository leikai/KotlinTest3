package org.czsxchinacourt.kotlintest3

import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.view.GravityCompat
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.forecast_item.view.*
import kotlinx.android.synthetic.main.now.*
import kotlinx.android.synthetic.main.suggestion.*
import kotlinx.android.synthetic.main.title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.czsxchinacourt.kotlintest3.bean.Weather
import org.czsxchinacourt.kotlintest3.utils.HttpUtil
import org.czsxchinacourt.kotlintest3.utils.Utility
import java.io.IOException

class WeatherActivity : AppCompatActivity() {
    var weatherId: String ?="中国"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 21){
            val decorView = window.decorView
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_weather)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString = prefs.getString("weather",null)

        if (weatherString != null){
            //有缓存时直接解析天气数据
            val weather = Utility.handleWeatherReaponse(weatherString)
           weatherId = weather?.basic.cityName
            showWeatherInfo(weather)


        }else{
            //无缓存时去服务器查询天气
            weatherId = intent.getStringExtra("weather_id")
            weatherLayout.visibility = View.INVISIBLE
            requestWeather(weatherId!!)
        }
        //设置刷新时的监听事件
        swipeRefresh.setOnRefreshListener {
            requestWeather(weatherId!!)
        }

        navButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val bingPic = prefs.getString("bing_pic",null)
        if (bingPic != null){
            //装载显示天气时的背景图
            val options = RequestOptions.placeholderOf(R.mipmap.ic_bg).error(R.mipmap.ic_bg)
            Glide.with(this)
                    .load(bingPic)
                    .apply(options)
                    .into(bingPicImg)
        }else{
            loadBingPic()
        }





    }

    private fun loadBingPic() {
        val url = "https://geekori.com/api/background/pic"
        HttpUtil.sendOkHttpRequest(url, object : okhttp3.Callback {
            override fun onFailure(call: Call?, e: IOException) {
                e!!.printStackTrace()
                runOnUiThread {
                    Glide.with(this@WeatherActivity).load(R.mipmap.ic_bg).into(bingPicImg)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bingPic = response!!.body()!!.string()
                val edit = PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                edit.putString("bing_pic",bingPic).apply()
                runOnUiThread{
                    val options = RequestOptions.placeholderOf(R.mipmap.ic_bg).error(R.mipmap.ic_bg)
                    Glide.with(this@WeatherActivity).load(bingPic).apply(options).into(bingPicImg)
                }
            }
        })


    }

    fun requestWeather(countyName:String) {
        val url =  "https://geekori.com/api/weather?id=${countyName}"
        HttpUtil.sendOkHttpRequest(url,object :Callback{
            override fun onFailure(call: Call?, e: IOException) {
                e!!.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@WeatherActivity,"",Toast.LENGTH_SHORT).show()
                    swipeRefresh.isRefreshing = false
                }
            }

            override fun onResponse(call: Call?, response: Response) {
                val responseString = response!!.body()!!.string()
                val weather = Utility.handleWeatherReaponse(responseString)
                runOnUiThread {
                    if (weather != null && "ok" == weather.status){
                        val edit = PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                        edit.putString("weather",responseString).apply()
                        weatherId = weather.basic.cityName
                        showWeatherInfo(weather)
                    }else{
                        Toast.makeText(this@WeatherActivity,"获取天气信息失败",Toast.LENGTH_SHORT).show()
                    }
                    swipeRefresh.isRefreshing = false
                }
            }
        })
    }

    private fun showWeatherInfo(weather: Weather) {
        val cityName = weather.basic.cityName
        val updateTime = weather.basic.update.updateTime.split(" ")[1]
        val degree = weather.now.tmperature + "℃"
        val weatherInfo = weather.now.more.info
        titleCity.text = cityName
        titleUpdateTime.text = updateTime
        degreeText.text = degree
        weatherInfoText.text = weatherInfo
        forecastLayout.removeAllViews()
        for (forcast in weather.forecastList) {
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false)
            view.dateText.text = forcast.date
            view.infoText.text = forcast.cond.txt_d
            view.maxText.text = forcast.tmp.max
            view.minText.text = forcast.tmp.min
            forecastLayout.addView(view)
        }
        if (weather.aqi != null) {
            aqiText.text = weather.aqi.city.aqi
            pm25Text.text = weather.aqi.city.pm25
        }else{
            aqiText.visibility=View.GONE
            pm25Text.visibility=View.GONE
        }
        comfortText.text = "舒适度：" + weather.suggestion.comfort.info
        carWeashText.text = "洗车指数：" + weather.suggestion.carWash.info
        sportText.text = "运动建议：" + weather.suggestion.sport.txt
        weatherLayout.visibility = View.VISIBLE

    }
}

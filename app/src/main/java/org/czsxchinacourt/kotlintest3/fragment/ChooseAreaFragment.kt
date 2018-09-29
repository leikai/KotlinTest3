package org.czsxchinacourt.kotlintest3.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import org.czsxchinacourt.kotlintest3.MainActivity
import org.czsxchinacourt.kotlintest3.R
import org.czsxchinacourt.kotlintest3.WeatherActivity
import org.czsxchinacourt.kotlintest3.bean.City
import org.czsxchinacourt.kotlintest3.bean.County
import org.czsxchinacourt.kotlintest3.bean.Province
import org.czsxchinacourt.kotlintest3.utils.DataSupport


class ChooseAreaFragment : Fragment() {

    /**
     * listview的数据源
     */
    private val dataList = ArrayList<String>()


    /**
     * 省列表
     */
    private var provinceList :List<Province>? = null

    /**
     * 市列表
     */
    private var cityList : List<City>? = null

    /**
     * 县区列表
     */
    private var countyList:List<County>? = null

    /**
     * 当前被选中的省份
     */
    private var selectedProvince :Province? = null

    /**
     * 当前被选中县市
     */
    private var selectedCity:City ? = null

    /**
     * 用于listview提供数据源的Adapter对象，数据源是数组
     */
    private var adapter:ArrayAdapter<String>? = null

    /**
     * 级别伴随对象
     */
    companion object{
        val LEVEL_PROVINCE = 0
        val LEVEL_CITY = 1
        val LEVEL_COUNTY = 2

    }

    /**
     * 当前被选中的级别
     */
    private var currentLevel:Int = 0

    private var handler: Handler =
    @SuppressLint("HandlerLeak")
    object : Handler(){
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.arg1){
                LEVEL_PROVINCE -> {
                    //在listView组件中显示列表
                    if (provinceList!!.size>0){
                        dataList.clear()
                        for (province in provinceList!!){
                            dataList.add(province.provinceName)
                        }
                        adapter!!.notifyDataSetChanged()
                        view!!.listView.setSelection(0)
                        currentLevel = LEVEL_PROVINCE
                    }
                }
                LEVEL_CITY ->{
                    if (cityList!!.size>0){
                        dataList.clear()
                        for (city in cityList!!){
                            dataList.add(city.cityName)
                        }
                        adapter!!.notifyDataSetChanged()
                        view!!.listView.setSelection(0)
                        currentLevel = LEVEL_CITY
                    }
                }
                LEVEL_COUNTY ->{
                    if (countyList!!.size>0){
                        dataList.clear()
                        for (county in countyList!!){
                            dataList.add(county.countyName)
                        }
                        adapter!!.notifyDataSetChanged()
                        view!!.listView.setSelection(0)
                        currentLevel = LEVEL_COUNTY
                    }
                }
            }
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =inflater!!.inflate(R.layout.fragment_item_list,container,false)
        adapter = ArrayAdapter(context,android.R.layout.simple_list_item_1,dataList)
        view.listView.adapter = adapter
        return view
    }

    /**
     * 当包含Fragment的Activity被创建时调用
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //设置Listview对象Item的点击事件
        view!!.listView.onItemClickListener = AdapterView.OnItemClickListener{parent, view, position, id ->
            //选择省
            if (currentLevel == LEVEL_PROVINCE){
                selectedProvince = provinceList!![position]
//                queryProvinces()
                queryCities()

            }else if (currentLevel == LEVEL_CITY){
                //选择市
                selectedCity = cityList!![position]
//                queryCities()
                queryCounties()
            }else if (currentLevel == LEVEL_COUNTY){
                //选择县区
                val countyName = countyList!![position].countyName
                //选择县区后，如果Fragment处于打开状态，则隐藏fragment，然后显示当前县区的天气状况
                if (activity is MainActivity){
                    startActivity(Intent(context, WeatherActivity::class.java).putExtra("weather_id",countyName))
                    (activity as MainActivity).finish()
                }else if (activity is WeatherActivity){
                    val activity = activity as WeatherActivity
                    activity.drawerLayout.closeDrawers()
                    activity.swipeRefresh.isRefreshing = true
                    activity.requestWeather(countyName)
                }

            }

        }
        //回退按钮的点击事件
        view!!.backButton.setOnClickListener{
            if (currentLevel == LEVEL_COUNTY){
                //当前处于县区级，回退到市级
                queryCities()
            }else if (currentLevel == LEVEL_CITY){
                //当前处于市级，回退到省级
                queryProvinces()
            }
        }
        queryProvinces()

    }

    /**
     * 查询所有省份
     */
    private fun queryProvinces(){
        view!!.titleText.text = "中国"
        view!!.backButton.visibility = View.GONE
        DataSupport.getProvinces {
            provinceList = it
            val msg = Message()
            msg.obj = this
            msg.arg1 = LEVEL_PROVINCE
            handler.sendMessage(msg)

        }
    }
    /**
     * 根据省份查询城市
     */
    private fun queryCities() {
        //!!表示如果为空抛出异常，非空的话就正常使用
        view!!.titleText.text = selectedProvince!!.provinceName
        view!!.backButton.visibility = View.VISIBLE
        DataSupport.getCities(selectedProvince!!.provinceCode){
            cityList = it
            val msg = Message()
            msg.obj = this
            msg.arg1 = LEVEL_CITY
            handler.sendMessage(msg)
        }
        DataSupport
    }

    /**
     * 根据选择的城市，查询县区
     */
    private fun queryCounties(){
        view!!.titleText.text = selectedCity!!.cityName
        view!!.backButton.visibility = View.VISIBLE
        DataSupport.getCounties(selectedProvince!!.provinceCode,selectedCity!!.cityCode){
            countyList = it
            val msg = Message()
            msg.obj = this
            msg.arg1 = LEVEL_COUNTY
            handler.sendMessage(msg)
        }
    }

}
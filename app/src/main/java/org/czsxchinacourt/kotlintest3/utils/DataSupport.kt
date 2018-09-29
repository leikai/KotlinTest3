package org.czsxchinacourt.kotlintest3.utils

import org.czsxchinacourt.kotlintest3.bean.City
import org.czsxchinacourt.kotlintest3.bean.County
import org.czsxchinacourt.kotlintest3.bean.Province
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object DataSupport {
    //从InputStream对象读取数据，并转换为ByteArray
    private fun getBytesByInputStream(content: InputStream):ByteArray{
        val bis = BufferedInputStream(content)
        val baos = ByteArrayOutputStream()
        val bos = BufferedOutputStream(baos)

        val buffer = ByteArray(1024*8)
        while (true){
            var lenth = bis.read(buffer)
            if (lenth<0){
                break
            }
            bos.write(buffer,0,lenth)
        }
        bos.flush()
        val bytes =baos.toByteArray()
        bos.close()
        bis.close()
        return bytes
    }
    //从服务器获取数据，并与字符串的形式返回获取的数据
    private fun getServerContent(urlStr: String):String{
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"//get请求方式
        conn.doInput = true  //默认也支持从服务器读取结果流
        conn.useCaches = false
        if (conn.responseCode == 200){
            val content = conn.inputStream
            //将inputStream转换成byte数组,getBytesByInputStream会关闭输入流
            val  responseBody = getBytesByInputStream(content)
            val string = kotlin.text.String(responseBody,Charset.forName("utf-8"))
            return string
        }
        return ""
    }
    //获取省列表
    fun getProvinces(provinceList:(List<Province>) -> Unit ){
        Thread{
            val content = getServerContent("https://geekori.com/api/china")
            val provinces = Utility.handleProviceReaponse(content)
            provinceList(provinces)
        }.start()

    }
    //根据省获取城市列表
    fun getCities(provinceCode: String, cityList: (List<City>) -> Unit){
        Thread{
            val content = getServerContent("https://geekori.com/api/china/${provinceCode}")
            val cities = Utility.handleCityReaponse(content,provinceCode)
            cityList(cities)
        }.start()
    }
    //根据城市获取县区数据
    fun getCounties(provinceCode: String, cityCode: String, countyList: (List<County>) ->Unit){
        Thread{
            val content = getServerContent("https://geekori.com/api/china/${provinceCode}/${cityCode}")
            //将县区Json数据转换成List<County>对象返回
            val counties = Utility.handleCountyReaponse(content,cityCode)
            countyList(counties)
        }.start()
    }


}
package com.iaruchkin.deepbreath.presentation.presenter

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.iaruchkin.deepbreath.App
import com.iaruchkin.deepbreath.common.AppPreferences
import com.iaruchkin.deepbreath.common.BasePresenter
import com.iaruchkin.deepbreath.network.dtos.AqiResponse
import com.iaruchkin.deepbreath.network.dtos.CityList
import com.iaruchkin.deepbreath.network.dtos.aqicnDTO.AqiData
import com.iaruchkin.deepbreath.network.dtos.findCityDTO.Data
import com.iaruchkin.deepbreath.network.parsers.FindCityApi
import com.iaruchkin.deepbreath.presentation.view.FindView
import com.iaruchkin.deepbreath.room.converters.ConverterAqi
import com.iaruchkin.deepbreath.room.entities.AqiEntity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

@InjectViewState
class FindPresenter : BasePresenter<FindView?>() {
    private val context = App.INSTANCE.applicationContext
    private var aqiEntity: List<AqiEntity?>? = null
    private var mCityList: List<Data?>? = null
    private val PRESENTER_WEATHER_TAG = "[list - presenter]"
    private val aqiCurrentLocation = "here"
    private val isGps = false

    override fun onFirstViewAttach() {
        loadData()
    }

    private fun loadData() {
//        loadAqiFromDb(aqiCurrentLocation)

        loadAqiFromNet("msk")
    }

    /**work with database
     *
     * @param geo
     */
    private fun loadAqiFromDb(geo: String) {
        val loadFromDb = Single.fromCallable {
            ConverterAqi
                    .getDataByParameter(context, geo)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ data: List<AqiEntity?>? -> updateAqiData(data, geo) }) { th: Throwable -> handleDbError(th) }
        disposeOnDestroy(loadFromDb)
        Log.i(PRESENTER_WEATHER_TAG, "Load AqiData from db")
        //        getViewState().showState(State.HasData);
    }

    /**check db responce
     *
     */
    private fun updateAqiData(data: List<AqiEntity?>?, parameter: String) {
        if (data!!.size == 0) {
            Log.w(PRESENTER_WEATHER_TAG, "there is no AqiData for location : $parameter")
            loadAqiFromNet(parameter)
        } else {
            aqiEntity = data
            updateAqi()
            Log.i(PRESENTER_WEATHER_TAG, "loaded AqiData from DB: " + data[0]!!.id + " / " + data[0]!!.aqi)
            Log.i(PRESENTER_WEATHER_TAG, "update AqiData executed on thread: " + Thread.currentThread().name)
            if (!isGps) {
                val lat = data[0]!!.locationLat
                val lon = data[0]!!.locationLon
                AppPreferences.setLocationDetails(context, lon, lat)
            }
        }
    }

    /**work with internet
     *
     * @param parameter
     */
    private fun loadAqiFromNet(parameter: String) {
        Log.i(PRESENTER_WEATHER_TAG, "Load AQI from net presenter")
        //        getViewState().showState(State.LoadingAqi);
        val disposable = FindCityApi.getInstance()
                .findCityEndpoint()
                .get(parameter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response: CityList ->
                    //                    updateAqiDB(response, "")
                    response.cityList
                    updateAqi()
                })
                { th: Throwable -> handleError(th) }

        disposeOnDestroy(disposable)
    }

    /**update database
     *
     * @param response
     * @param parameter
     */
    private fun updateAqiDB(response: AqiResponse, parameter: String) {
        if (response.aqiData == null) { //            getViewState().showState(State.HasNoData);
            Log.w(PRESENTER_WEATHER_TAG, "no data!")
        } else {
            val saveDataToDb = Single.fromCallable { response.aqiData }
                    .subscribeOn(Schedulers.io())
                    .map { aqiDTO: AqiData? ->
                        ConverterAqi.saveAllDataToDb(context,
                                ConverterAqi.dtoToDao(aqiDTO, parameter), parameter)
                        ConverterAqi.getDataByParameter(context, parameter)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { aqiEntities: List<AqiEntity?> ->
                                aqiEntity = aqiEntities
                                updateAqi()
                                Log.i(PRESENTER_WEATHER_TAG, "loaded aqi from NET to DB, size: " + aqiEntities.size)
                            }) { th: Throwable -> handleDbError(th) }
            disposeOnDestroy(saveDataToDb)
            //            getViewState().showState(State.HasData);
        }
    }

    /**setting data objects
     *
     */
    private fun updateAqi() {
        if (mCityList != null) {
            viewState!!.showCityList(mCityList!!)
            //            getViewState().showState(State.HasData);
        }
    }

    /**handling errors
     *
     * @param th
     */
    private fun handleError(th: Throwable) { //        getViewState().showState(State.NetworkError);
        loadAQIFromDb()
        Log.e(PRESENTER_WEATHER_TAG, th.message, th)
    }

    private fun loadAQIFromDb() {
        val loadFromDb = Single.fromCallable {
            ConverterAqi
                    .getLastData(context)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { data: List<AqiEntity?>? ->
                            aqiEntity = data
                            updateAqi()
                        }) { th: Throwable -> handleDbError(th) }
        disposeOnDestroy(loadFromDb)
        Log.e(PRESENTER_WEATHER_TAG, "Load AQIData from db")
        //        getViewState().showState(State.HasData);
    }

    private fun handleDbError(th: Throwable) { //        getViewState().showState(State.DbError);
        Log.e(PRESENTER_WEATHER_TAG, th.message, th)
    }
}
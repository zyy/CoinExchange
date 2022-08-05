package com.bizzan.bitrade.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bizzan.bitrade.entity.Coin;
import com.bizzan.bitrade.entity.CoinThumb;
import com.bizzan.bitrade.processor.CoinProcessor;
import com.bizzan.bitrade.processor.CoinProcessorFactory;
import com.bizzan.bitrade.service.CoinService;
import com.bizzan.bitrade.service.ExchangeCoinService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 币种汇率管理
 */
@Component
@Slf4j
@ToString
public class CoinExchangeRate {
    @Getter
    @Setter
    private BigDecimal usdCnyRate = new BigDecimal("6.45");

    @Getter
    @Setter
    private BigDecimal usdtCnyRate = new BigDecimal("6.42");

    @Getter
    @Setter
    private BigDecimal usdJpyRate = new BigDecimal("110.02");
    @Getter
    @Setter
    private BigDecimal usdHkdRate = new BigDecimal("7.8491");
    @Getter
    @Setter
    private BigDecimal sgdCnyRate = new BigDecimal("5.08");
    @Setter
    private CoinProcessorFactory coinProcessorFactory;


    private Map<String,BigDecimal> ratesMap = new HashMap<String,BigDecimal>(){{
        put("CNY",new BigDecimal("6.36"));
        put("TWD",new BigDecimal("6.40"));
        put("USD",new BigDecimal("1.00"));
        put("EUR",new BigDecimal("0.91"));
        put("HKD",new BigDecimal("7.81"));
        put("SGD",new BigDecimal("1.36"));
    }};

    @Autowired
    private CoinService coinService;
    @Autowired
    private ExchangeCoinService exCoinService;


    public BigDecimal getUsdRate(String symbol) {
        log.info("CoinExchangeRate getUsdRate unit = " + symbol);
        if ("USDT".equalsIgnoreCase(symbol)) {
            log.info("CoinExchangeRate getUsdRate unit = USDT  ,result = ONE");
            return BigDecimal.ONE;
        } else if ("CNY".equalsIgnoreCase(symbol)) {
            log.info("CoinExchangeRate getUsdRate unit = CNY  ,result : 1 divide {}", this.usdtCnyRate);
            BigDecimal bigDecimal = BigDecimal.ONE.divide(usdtCnyRate, 4,BigDecimal.ROUND_DOWN).setScale(4, BigDecimal.ROUND_DOWN);
            return bigDecimal;
        }else if ("BITCNY".equalsIgnoreCase(symbol)) {
            BigDecimal bigDecimal = BigDecimal.ONE.divide(usdCnyRate, 4,BigDecimal.ROUND_DOWN).setScale(4, BigDecimal.ROUND_DOWN);
            return bigDecimal;
        } else if ("ET".equalsIgnoreCase(symbol)) {
            BigDecimal bigDecimal = BigDecimal.ONE.divide(usdCnyRate, 4,BigDecimal.ROUND_DOWN).setScale(4, BigDecimal.ROUND_DOWN);
            return bigDecimal;
        } else if ("JPY".equalsIgnoreCase(symbol)) {
            BigDecimal bigDecimal = BigDecimal.ONE.divide(usdJpyRate, 4,BigDecimal.ROUND_DOWN).setScale(4, BigDecimal.ROUND_DOWN);
            return bigDecimal;
        }else if ("HKD".equalsIgnoreCase(symbol)) {
            BigDecimal bigDecimal = BigDecimal.ONE.divide(usdHkdRate, 4,BigDecimal.ROUND_DOWN).setScale(4, BigDecimal.ROUND_DOWN);
            return bigDecimal;
        }
        String usdtSymbol = symbol.toUpperCase() + "/USDT";
        String btcSymbol = symbol.toUpperCase() + "/BTC";
        String ethSymbol = symbol.toUpperCase() + "/ETH";

        if (coinProcessorFactory != null) {
            if (coinProcessorFactory.containsProcessor(usdtSymbol)) {
                log.info("Support exchange coin = {}", usdtSymbol);
                CoinProcessor processor = coinProcessorFactory.getProcessor(usdtSymbol);
                if(processor == null) {
                    return BigDecimal.ZERO;
                }
                CoinThumb thumb = processor.getThumb();
                if(thumb == null) {
                    log.info("Support exchange coin thumb is null", thumb);
                    return BigDecimal.ZERO;
                }
                return thumb.getUsdRate();
            } else if (coinProcessorFactory.containsProcessor(btcSymbol)) {
                log.info("Support exchange coin = {}/BTC", btcSymbol);
                CoinProcessor processor = coinProcessorFactory.getProcessor(btcSymbol);
                if(processor == null) {
                    return BigDecimal.ZERO;
                }
                CoinThumb thumb = processor.getThumb();
                if(thumb == null) {
                    log.info("Support exchange coin thumb is null", thumb);
                    return BigDecimal.ZERO;
                }
                return thumb.getUsdRate();
            } else if (coinProcessorFactory.containsProcessor(ethSymbol)) {
                log.info("Support exchange coin = {}/ETH", ethSymbol);
                CoinProcessor processor = coinProcessorFactory.getProcessor(ethSymbol);
                if(processor == null) {
                    return BigDecimal.ZERO;
                }
                CoinThumb thumb = processor.getThumb();
                if(thumb == null) {
                    log.info("Support exchange coin thumb is null", thumb);
                    return BigDecimal.ZERO;
                }
                return thumb.getUsdRate();
            } else {
                return getDefaultUsdRate(symbol);
            }
        } else {
            return getDefaultUsdRate(symbol);
        }
    }

    /**
     * 获取币种设置里的默认价格
     *
     * @param symbol
     * @return
     */
    public BigDecimal getDefaultUsdRate(String symbol) {
        Coin coin = coinService.findByUnit(symbol);
        if (coin != null) {
            return new BigDecimal(coin.getUsdRate());
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getCnyRate(String symbol) {
        if ("CNY".equalsIgnoreCase(symbol)) {
            return BigDecimal.ONE;
        } else if("ET".equalsIgnoreCase(symbol)){
            return BigDecimal.ONE;
        }
        return getUsdRate(symbol).multiply(usdtCnyRate).setScale(2, RoundingMode.DOWN);
    }



    public BigDecimal getJpyRate(String symbol) {
        if ("JPY".equalsIgnoreCase(symbol)) {
            return BigDecimal.ONE;
        }
        return getUsdRate(symbol).multiply(usdJpyRate).setScale(2, RoundingMode.DOWN);
    }

    public BigDecimal getHkdRate(String symbol) {
        if ("HKD".equalsIgnoreCase(symbol)) {
            return BigDecimal.ONE;
        }
        return getUsdRate(symbol).multiply(usdHkdRate).setScale(2, RoundingMode.DOWN);
    }




//
//    public static void main(String[] args) {
//        Map<String,BigDecimal> ratesMap = new HashMap<String,BigDecimal>(){{
//            put("CNY",new BigDecimal("6.36"));
//            put("TWD",new BigDecimal("6.40"));
//            put("USD",new BigDecimal("1.00"));
//            put("EUR",new BigDecimal("0.91"));
//            put("HKD",new BigDecimal("7.81"));
//            put("SGD",new BigDecimal("1.36"));
//        }};
//        Set<String> currencies = ratesMap.keySet();
//        for (String currency : currencies) {
//            // okex接口
//            String urlOk="https://www.okex.com/v3/c2c/otc-ticker?&baseCurrency=USDT&quoteCurrency="+currency;
//            try {
//                HttpHost proxy = new HttpHost("127.0.0.1", 7890);
//                Unirest.setProxy(proxy);
//                HttpResponse<JsonNode> resp = Unirest.get(urlOk).asJson();
//                if(resp.getStatus() == 200) { //正确返回
//                    JSONObject ret = JSON.parseObject(resp.getBody().toString());
//                    if(ret.getIntValue("code") == 0) {
//                        double doubleValue = ret.getJSONObject("data").getDoubleValue("otcTicker");
//                        ratesMap.put(currency,new BigDecimal(doubleValue).setScale(2, RoundingMode.HALF_UP));
//                    }
//                }
//            } catch (UnirestException e) {
//                log.info("开始同步OTC报错");
//                log.error(e.toString());
//                e.printStackTrace();
//            }
//        }
//
//        System.out.println(JSON.toJSONString(ratesMap));
////
//
//    }

    /**
     * 每5分钟同步一次价格
     *
     * @throws UnirestException
     */

    @Scheduled(cron = "0 */5 * * * *")
    public void syncUsdtCnyPrice() {
        log.info("开始同步OTC");
        // okex接口
//        String urlOk="https://www.okex.com/v3/c2c/otc-ticker?&baseCurrency=USDT&quoteCurrency="+"CNY";
//        try {
//            HttpResponse<JsonNode> resp = Unirest.get(urlOk).asJson();
//            if(resp.getStatus() == 200) { //正确返回
//                JSONObject ret = JSON.parseObject(resp.getBody().toString());
//                if(ret.getIntValue("code") == 0) {
//                    double doubleValue = ret.getJSONObject("data").getDoubleValue("otcTicker");
//                    System.out.println("otc-----"+doubleValue);
//                    setUsdtCnyRate(new BigDecimal(doubleValue).setScale(2, RoundingMode.HALF_UP));
//                    return;
//                }
//            }
//        } catch (UnirestException e) {
//            log.error(e.toString());
//            e.printStackTrace();
//        }

        Set<String> currencies = ratesMap.keySet();
        for (String currency : currencies) {
            // okex接口
            String urlOk="https://www.okex.com/v3/c2c/otc-ticker?&baseCurrency=USDT&quoteCurrency="+currency;
            try {
                HttpResponse<JsonNode> resp = Unirest.get(urlOk).asJson();
                if(resp.getStatus() == 200) { //正确返回
                    JSONObject ret = JSON.parseObject(resp.getBody().toString());
                    if(ret.getIntValue("code") == 0) {
                        double doubleValue = ret.getJSONObject("data").getDoubleValue("otcTicker");
                        ratesMap.put(currency,new BigDecimal(doubleValue).setScale(2, RoundingMode.HALF_UP));
                    }
                }
            } catch (UnirestException e) {
                log.info("开始同步OTC报错");
                log.error(e.toString());
                e.printStackTrace();
            }
        }

        // HuobiOTC接口
        String url = "https://otc-api.huobi.com/v1/data/market/detail";
        //如有报错 请自行官网申请获取汇率 或者默认写死
        try {
            HttpResponse<JsonNode> resp = Unirest.get(url).asJson();
            if(resp.getStatus() == 200) { //正确返回
                JSONObject ret = JSON.parseObject(resp.getBody().toString());
                if(ret.getIntValue("code") == 200) {
                    JSONArray array = ret.getJSONObject("data").getJSONArray("detail");
                    for(int i=0; i<array.size(); i++) {
                        JSONObject json = array.getJSONObject(i);
                        if("USDT".equalsIgnoreCase(json.getString("coinName"))) {
                            setUsdtCnyRate(new BigDecimal(json.getString("buy")).setScale(2, RoundingMode.HALF_UP));
                            return;
                        }
                    }
                }
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }


        // Huobi Otc接口（如抹茶接口无效则走此路径）
        String url2 = "https://otc-api-hk.eiijo.cn/v1/data/trade-market?coinId=2&currency=1&tradeType=sell&currPage=1&payMethod=0&country=37&blockType=general&online=1&range=0&amount=";
        try {
            HttpResponse<JsonNode> resp2 = Unirest.get(url2).asJson();
            if(resp2.getStatus() == 200) { //正确返回
                JSONObject ret2 = JSON.parseObject(resp2.getBody().toString());
                if(ret2.getIntValue("code") == 200) {
                    JSONArray arr = ret2.getJSONArray("data");
                    if(arr.size() > 0) {
                        JSONObject obj = arr.getJSONObject(0);
                        setUsdtCnyRate(new BigDecimal(obj.getDouble("price")).setScale(2, RoundingMode.HALF_UP));
                        return;
                    }
                }
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }


        // Okex Otc接口
        String url3 = "https://www.okex.com/v3/c2c/tradingOrders/book?t=1566269221580&side=sell&baseCurrency=usdt&quoteCurrency=cny&userType=certified&paymentMethod=all";
        try {
            HttpResponse<JsonNode> resp3 = Unirest.get(url2).asJson();
            if(resp3.getStatus() == 200) { //正确返回
                JSONObject ret3 = JSON.parseObject(resp3.getBody().toString());
                if(ret3.getIntValue("code") == 0) {
                    JSONObject okObj = ret3.getJSONObject("data");
                    JSONArray okArr = okObj.getJSONArray("sell");
                    if(okArr.size() > 0) {
                        JSONObject okObj2 = okArr.getJSONObject(0);
                        setUsdtCnyRate(new BigDecimal(okObj2.getDouble("price")).setScale(2, RoundingMode.HALF_UP));
                        return;
                    }
                }
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }

    }

    /**
     * 每30分钟同步一次价格
     *
     * @throws UnirestException
     */

    @Scheduled(cron = "0 */30 * * * *")
    public void syncPrice() throws UnirestException {

        String url = "http://web.juhe.cn:8080/finance/exchange/frate?key=0330f6e51631ee1c0c4696a49201cb94";
        //如有报错 请自行官网申请获取汇率 或者默认写死
        HttpResponse<JsonNode> resp = Unirest.get(url)
                .queryString("key", "0330f6e51631ee1c0c4696a49201cb94")
                .asJson();
        log.info("forex result:{}", resp.getBody());
        JSONObject ret = JSON.parseObject(resp.getBody().toString());

        if(ret.getIntValue("resultcode") == 200) {
            JSONArray result = ret.getJSONArray("result");
            result.forEach(json -> {
                JSONObject obj = (JSONObject) json;
                if ("USDCNY".equals(obj.getString("code"))) {
                    setUsdCnyRate(new BigDecimal(obj.getDouble("price")).setScale(2, RoundingMode.DOWN));
                    log.info(obj.toString());
                } else if ("USDJPY".equals(obj.getString("code"))) {
                    setUsdJpyRate(new BigDecimal(obj.getDouble("price")).setScale(2, RoundingMode.DOWN));
                    log.info(obj.toString());
                }
                else if ("USDHKD".equals(obj.getString("code"))) {
                    setUsdHkdRate(new BigDecimal(obj.getDouble("price")).setScale(2, RoundingMode.DOWN));
                    log.info(obj.toString());
                }
	            /*
	            else if("SGDCNH".equals(obj.getString("code"))){
	                setSgdCnyRate(new BigDecimal(obj.getDouble("price")).setScale(2,RoundingMode.DOWN));
	                log.info(obj.toString());
	            }
	            */

            });
        }
    }

    public Map<String, BigDecimal> getAllRate(String symbol) {
        Map<String,BigDecimal> result = new HashMap<>();
        for (String currency : ratesMap.keySet()) {
            if ("CNY".equalsIgnoreCase(symbol)) {
                result.put(currency,BigDecimal.ONE);
                continue;
            } else if("ET".equalsIgnoreCase(symbol)){
                result.put(currency,BigDecimal.ONE);
                continue;
            }
            BigDecimal usdtRate = ratesMap.get(currency);
            BigDecimal rate = getUsdRate(symbol).multiply(usdtRate).setScale(2, RoundingMode.DOWN);
            result.put(currency,rate);
        }
        return result;
    }
}

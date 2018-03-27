package com.github.galleog.sk.statistics.client;

import com.github.galleog.sk.statistics.domain.Currency;
import com.github.galleog.sk.statistics.domain.ExchangeRatesContainer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(url = "${rates.url}", name = "rates-client")
public interface ExchangeRatesClient {

    @RequestMapping(method = RequestMethod.GET, value = "/latest")
    ExchangeRatesContainer getRates(@RequestParam("base") Currency base);

}

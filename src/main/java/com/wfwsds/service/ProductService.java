package com.wfwsds.service;

import com.wfwsds.adapter.ExternalDataResAdapter;
import com.wfwsds.model.ErrorRes;
import com.wfwsds.model.ExternalDataRes;
import com.wfwsds.model.ProductDto;
import com.wfwsds.model.UserReq;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ProductService {

  private static final String ADAPTER_SUFFIX = "Adapter";

  private final ConnectionFactory connectionFactory;

  private final Function<ExternalDataRes, ExternalDataResAdapter> resolveAdapter;
  private final UnaryOperator<ExternalDataRes> postProcess;

  @Autowired
  public ProductService(ApplicationContext context,
      ConnectionFactory connectionFactory) {

    this.connectionFactory = connectionFactory;

    this.resolveAdapter = v ->
        ((ExternalDataResAdapter) context.getBean(v.getClass().getSimpleName()
            + ADAPTER_SUFFIX));
    this.postProcess = response ->
        resolveAdapter.apply(response).postProcess(response);
  }

  public Mono<ProductDto> process() {

    // TODO: 1. set up monos list which will be called in parallel
    //  based on Req & CustomerConfiguration information
    List<Mono<ExternalDataRes>> monos = setUpMonosList(/*req, customerConfig, etc.*/);

    // TODO: 2. make a call for CreditFile (to DataService Layer)
    // TODO: 3. make a parallel calls to External Data Sources
    Mono<ProductDto> mono = aggregateResults(monos);
    // TODO: 4. set up processor list based on Req, CustomerConfiguration information and CreditFile
    // TODO: 5. make a call for External Data Sources dependant on CreditFile
    // TODO: 6. aggregate the resulted monos into ProductDto

    return mono;
  }

  private List<Mono<ExternalDataRes>> setUpMonosList() {

    List<Mono<ExternalDataRes>> monos = new ArrayList<>();

    monos.add((Mono<ExternalDataRes>) connectionFactory
        .postUserData(new UserReq("100500", "testName", "testLastName")));
    monos.add((Mono<ExternalDataRes>) connectionFactory.statisticData());
    monos.add((Mono<ExternalDataRes>) connectionFactory.creditCardData());
    monos.add((Mono<ExternalDataRes>) connectionFactory.bookData());

    return monos;
  }

  private Mono<ProductDto> aggregateResults(List<Mono<ExternalDataRes>> monos) {

    return Mono.zip(monos,
        resArray -> {

          ProductDto productDto = new ProductDto();
          for (Object o : resArray) {
            if (o instanceof ExternalDataRes){
              ExternalDataRes res = (ExternalDataRes) o;
              productDto.concat(postProcess.apply(res));
            }
          }
          return productDto;
        }
    );

  }
}
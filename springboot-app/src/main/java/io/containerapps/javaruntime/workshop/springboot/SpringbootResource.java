// tag::adocHeader[]
package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.annotation.QueryValue;

import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.invoke.MethodHandles.lookup;

@Controller("/springboot")
public class SpringbootResource {

    private static final Logger LOGGER = System.getLogger(lookup().lookupClass().getName());

    private final StatisticsRepository repository;

    public SpringbootResource(StatisticsRepository statisticsRepository) {
        this.repository = statisticsRepository;
    }
// end::adocHeader[]

    /**
     * Says hello.
     * {@code curl 'localhost:8703/springboot'}
     *
     * @return hello
     */
// tag::adocMethodHello[]
    @Get(produces = MediaType.TEXT_PLAIN + ";charset=UTF-8")
    public HttpResponse<String> hello() {
        LOGGER.log(INFO, "Spring Boot: hello");
        String greeting = "Spring Boot: hello";
        // The entity headers are stated on the response itself so that a HEAD request, whose body
        // is discarded before it is written, still describes the entity a GET would have returned.
        return HttpResponse.ok(greeting)
                .contentType(MediaType.TEXT_PLAIN + ";charset=UTF-8")
                .contentLength(greeting.getBytes(StandardCharsets.UTF_8).length);
    }
// end::adocMethodHello[]

    /**
     * Reports which methods the greeting supports.
     */
    @Options
    public HttpResponse<?> helloOptions() {
        return HttpResponse.ok().header(HttpHeaders.ALLOW, "GET,HEAD,OPTIONS");
    }

    /**
     * Simulates requests that use a lot of CPU.
     * {@code curl 'localhost:8703/springboot/cpu'}
     * {@code curl 'localhost:8703/springboot/cpu?iterations=10'}
     * {@code curl 'localhost:8703/springboot/cpu?iterations=10&db=true'}
     * {@code curl 'localhost:8703/springboot/cpu?iterations=10&db=true&desc=java17'}
     *
     * @param iterations the number of iterations to run (times 20,000).
     * @return the result
     */
// tag::adocMethodCPU[]
    @Get(uri = "/cpu", produces = MediaType.TEXT_PLAIN + ";charset=UTF-8")
    public String cpu(@QueryValue(value = "iterations", defaultValue = "10") Long iterations,
                      @QueryValue(value = "db", defaultValue = "false") Boolean db,
                      @Nullable @QueryValue(value = "desc") String desc) {
        LOGGER.log(INFO, "Spring Boot: cpu: {0} {1} with desc {2}", iterations, db, desc);
        Long iterationsDone = iterations;

        Instant start = Instant.now();
        if (iterations == null) {
            iterations = 20000L;
        } else {
            iterations *= 20000;
        }
        while (iterations > 0) {
            if (iterations % 20000 == 0) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                }
            }
            iterations--;
        }

        if (db) {
            Statistics statistics = new Statistics();
            statistics.type = Type.CPU;
            statistics.parameter = iterations.toString();
            statistics.duration = Duration.between(start, Instant.now());
            statistics.description = desc;
            repository.save(statistics);
        }

        String msg = "Spring Boot: CPU consumption is done with " + iterationsDone + " iterations in " + Duration.between(start, Instant.now()).getNano() + " nano-seconds.";
        if (db) {
            msg += " The result is persisted in the database.";
        }
        return msg;
    }
// end::adocMethodCPU[]

    /**
     * Simulates requests that use a lot of memory.
     * {@code curl 'localhost:8703/springboot/memory'}
     * {@code curl 'localhost:8703/springboot/memory?bites=10'}
     * {@code curl 'localhost:8703/springboot/memory?bites=10&db=true'}
     * {@code curl 'localhost:8703/springboot/memory?bites=10&db=true&desc=java17'}
     *
     * @param bites the number of megabytes to eat
     * @return the result.
     */
// tag::adocMethodMemory[]
    @Get(uri = "/memory", produces = MediaType.TEXT_PLAIN + ";charset=UTF-8")
    public String memory(@QueryValue(value = "bites", defaultValue = "10") Integer bites,
                         @QueryValue(value = "db", defaultValue = "false") Boolean db,
                         @Nullable @QueryValue(value = "desc") String desc) {
        LOGGER.log(INFO, "Spring Boot: memory: {0} {1} with desc {2}", bites, db, desc);

        Instant start = Instant.now();
        if (bites == null) {
            bites = 1;
        }
        HashMap hunger = new HashMap<>();
        for (int i = 0; i < bites * 1024 * 1024; i += 8192) {
            byte[] bytes = new byte[8192];
            hunger.put(i, bytes);
            for (int j = 0; j < 8192; j++) {
                bytes[j] = '0';
            }
        }

        if (db) {
            Statistics statistics = new Statistics();
            statistics.type = Type.MEMORY;
            statistics.parameter = bites.toString();
            statistics.duration = Duration.between(start, Instant.now());
            statistics.description = desc;
            repository.save(statistics);
        }

        String msg = "Spring Boot: Memory consumption is done with " + bites + " bites in " + Duration.between(start, Instant.now()).getNano() + " nano-seconds.";
        if (db) {
            msg += " The result is persisted in the database.";
        }
        return msg;
    }
// end::adocMethodMemory[]

    /**
     * Returns what's in the database.
     * {@code curl 'localhost:8703/springboot/stats'}
     *
     * @return the list of Statistics.
     */
// tag::adocMethodStats[]
    @Get(uri = "/stats", produces = MediaType.APPLICATION_JSON)
    public List<Statistics> stats() {
        LOGGER.log(INFO, "Spring Boot: retrieving statistics");
        List<Statistics> result = new ArrayList<Statistics>();
        for (Statistics stats : repository.findAll()) {
            result.add(stats);
        }
        return result;
    }
// end::adocMethodStats[]
}

/* Copyright (c) 2020 OpenJAX
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.openjax.json;

import java.io.IOException;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.SampleTime)
public class JsonReaderRTest extends AbstractTest {
  @Test
  public void test() throws RunnerException {
    final Options options = new OptionsBuilder()
      .include(".*" + JsonReaderRTest.class.getSimpleName() + ".*")
      .warmupIterations(1)
//      .addProfiler(HotspotMemoryProfiler.class)
      .measurementIterations(5)
      .forks(1)
      .build();

    new Runner(options).run();
  }

  @Benchmark
  public void testTweets() throws IOException {
    assertPass("tweets.json");
  }

  @Benchmark
  public void testPaypal() throws IOException {
    assertPass("paypal.json");
  }

  @Benchmark
  public void testWebapp() throws IOException {
    assertPass("webapp.json");
  }

  @Benchmark
  public void testVatrates() throws IOException {
    assertPass("vatrates.json");
  }

  @Benchmark
  public void testEmployees() throws IOException {
    assertPass("employees.json");
  }

  @Benchmark
  public void testEarthquakes() throws IOException {
    assertPass("earthquakes.json");
  }

  @Benchmark
  public void testDiabetes() throws IOException {
    assertPass("diabetes.json");
  }

  @Benchmark
  public void testDcat() throws IOException {
    assertPass("dcat.json");
  }

  @Benchmark
  public void testShowtimes() throws IOException {
    assertPass("showtimes.json");
  }

  @Benchmark
  public void testCrime() throws IOException {
    assertPass("crime.json");
  }

  @Benchmark
  public void testSuicide() throws IOException {
    assertPass("suicide.json");
  }

  @Benchmark
  public void testSample() throws IOException {
    assertPass("sample.json");
  }

  @Benchmark
  public void testInsurers() throws IOException {
    assertPass("insurers.json");
  }

  @Benchmark
  public void testHgbp() throws IOException {
    assertPass("hgbp.json");
  }

  @Benchmark
  public void testGithub() throws IOException {
    assertPass("github.json");
  }

  @Benchmark
  public void testCensusDiversity() throws IOException {
    assertPass("census-diversity.json");
  }

  @Benchmark
  public void testReddit() throws IOException {
    assertPass("reddit.json");
  }

  @Benchmark
  public void testSat() throws IOException {
    assertPass("sat.json");
  }

  @Benchmark
  public void testGiphy() throws IOException {
    assertPass("giphy.json");
  }

  @Benchmark
  public void testJobs() throws IOException {
    assertPass("jobs.json");
  }

  @Benchmark
  public void testPowerball() throws IOException {
    assertPass("powerball.json");
  }

  @Benchmark
  public void testDemographics() throws IOException {
    assertPass("demographics.json");
  }

  @Benchmark
  public void testComplaints() throws IOException {
    assertPass("complaints.json");
  }

  @Benchmark
  public void testGraduation() throws IOException {
    assertPass("graduation.json");
  }

  @Benchmark
  public void testNobel() throws IOException {
    assertPass("nobel.json");
  }

  @Benchmark
  public void testAstronauts() throws IOException {
    assertPass("astronauts.json");
  }

  @Benchmark
  public void testMega() throws IOException {
    assertPass("mega.json");
  }

  @Benchmark
  public void testMarijuana() throws IOException {
    assertPass("marijuana.json");
  }

  @Benchmark
  public void testPets() throws IOException {
    assertPass("pets.json");
  }

  @Benchmark
  public void testRestaurants() throws IOException {
    assertPass("restaurants.json");
  }

  @Benchmark
  public void testOpportunityZones() throws IOException {
    assertPass("opportunity_zones.json");
  }

  @Benchmark
  public void testSalesTax() throws IOException {
    assertPass("sales_tax.json");
  }

  @Benchmark
  public void testZipcodes() throws IOException {
    assertPass("zipcodes.json");
  }

  @Benchmark
  public void testDatasets() throws IOException {
    assertPass("datasets.json");
  }

  @Benchmark
  public void testNames() throws IOException {
    assertPass("names.json");
  }

  @Benchmark
  public void testNeighborhoods() throws IOException {
    assertPass("neighborhoods.json");
  }

  @Benchmark
  public void testCensusTracts() throws IOException {
    assertPass("census-tracts.json");
  }

  @Benchmark
  public void testWeather() throws IOException {
    assertPass("weather.json");
  }

  @Benchmark
  public void testSalaries() throws IOException {
    assertPass("salaries.json");
  }

  @Benchmark
  public void testMovies() throws IOException {
    assertPass("movies.json");
  }

  @Benchmark
  public void testDoe() throws IOException {
    assertPass("doe.json");
  }

  @Benchmark
  public void testNhanes() throws IOException {
    assertPass("nhanes.json");
  }

  @Benchmark
  public void testNutrition() throws IOException {
    assertPass("nutrition.json");
  }

  @Benchmark
  public void testAccidentalDeath() throws IOException {
    assertPass("accidental-death.json");
  }

  @Benchmark
  public void testBusinesses() throws IOException {
    assertPass("businesses.json");
  }

  @Benchmark
  public void testTravel() throws IOException {
    assertPass("travel.json");
  }

  @Benchmark
  public void testCensusBoundaries() throws IOException {
    assertPass("census-boundaries.json");
  }

  @Benchmark
  public void testAnimalServices() throws IOException {
    assertPass("animal-services.json");
  }

  @Benchmark
  public void testCrashes() throws IOException {
    assertPass("crashes.json");
  }

  @Benchmark
  public void testIncidents() throws IOException {
    assertPass("incidents.json");
  }
}
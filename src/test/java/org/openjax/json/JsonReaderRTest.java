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
    passFile("tweets.json");
  }

  @Benchmark
  public void testPaypal() throws IOException {
    passFile("paypal.json");
  }

  @Benchmark
  public void testWebapp() throws IOException {
    passFile("webapp.json");
  }

  @Benchmark
  public void testVatrates() throws IOException {
    passFile("vatrates.json");
  }

  @Benchmark
  public void testEmployees() throws IOException {
    passFile("employees.json");
  }

  @Benchmark
  public void testEarthquakes() throws IOException {
    passFile("earthquakes.json");
  }

  @Benchmark
  public void testDiabetes() throws IOException {
    passFile("diabetes.json");
  }

  @Benchmark
  public void testDcat() throws IOException {
    passFile("dcat.json");
  }

  @Benchmark
  public void testShowtimes() throws IOException {
    passFile("showtimes.json");
  }

  @Benchmark
  public void testCrime() throws IOException {
    passFile("crime.json");
  }

  @Benchmark
  public void testSuicide() throws IOException {
    passFile("suicide.json");
  }

  @Benchmark
  public void testSample() throws IOException {
    passFile("sample.json");
  }

  @Benchmark
  public void testInsurers() throws IOException {
    passFile("insurers.json");
  }

  @Benchmark
  public void testHgbp() throws IOException {
    passFile("hgbp.json");
  }

  @Benchmark
  public void testGithub() throws IOException {
    passFile("github.json");
  }

  @Benchmark
  public void testCensusDiversity() throws IOException {
    passFile("census-diversity.json");
  }

  @Benchmark
  public void testReddit() throws IOException {
    passFile("reddit.json");
  }

  @Benchmark
  public void testSat() throws IOException {
    passFile("sat.json");
  }

  @Benchmark
  public void testGiphy() throws IOException {
    passFile("giphy.json");
  }

  @Benchmark
  public void testJobs() throws IOException {
    passFile("jobs.json");
  }

  @Benchmark
  public void testPowerball() throws IOException {
    passFile("powerball.json");
  }

  @Benchmark
  public void testDemographics() throws IOException {
    passFile("demographics.json");
  }

  @Benchmark
  public void testComplaints() throws IOException {
    passFile("complaints.json");
  }

  @Benchmark
  public void testGraduation() throws IOException {
    passFile("graduation.json");
  }

  @Benchmark
  public void testNobel() throws IOException {
    passFile("nobel.json");
  }

  @Benchmark
  public void testAstronauts() throws IOException {
    passFile("astronauts.json");
  }

  @Benchmark
  public void testMega() throws IOException {
    passFile("mega.json");
  }

  @Benchmark
  public void testMarijuana() throws IOException {
    passFile("marijuana.json");
  }

  @Benchmark
  public void testPets() throws IOException {
    passFile("pets.json");
  }

  @Benchmark
  public void testRestaurants() throws IOException {
    passFile("restaurants.json");
  }

  @Benchmark
  public void testOpportunityZones() throws IOException {
    passFile("opportunity_zones.json");
  }

  @Benchmark
  public void testSalesTax() throws IOException {
    passFile("sales_tax.json");
  }

  @Benchmark
  public void testZipcodes() throws IOException {
    passFile("zipcodes.json");
  }

  @Benchmark
  public void testDatasets() throws IOException {
    passFile("datasets.json");
  }

  @Benchmark
  public void testNames() throws IOException {
    passFile("names.json");
  }

  @Benchmark
  public void testNeighborhoods() throws IOException {
    passFile("neighborhoods.json");
  }

  @Benchmark
  public void testCensusTracts() throws IOException {
    passFile("census-tracts.json");
  }

  @Benchmark
  public void testWeather() throws IOException {
    passFile("weather.json");
  }

  @Benchmark
  public void testSalaries() throws IOException {
    passFile("salaries.json");
  }

  @Benchmark
  public void testMovies() throws IOException {
    passFile("movies.json");
  }

  @Benchmark
  public void testDoe() throws IOException {
    passFile("doe.json");
  }

  @Benchmark
  public void testNhanes() throws IOException {
    passFile("nhanes.json");
  }

  @Benchmark
  public void testNutrition() throws IOException {
    passFile("nutrition.json");
  }

  @Benchmark
  public void testAccidentalDeath() throws IOException {
    passFile("accidental-death.json");
  }

  @Benchmark
  public void testBusinesses() throws IOException {
    passFile("businesses.json");
  }

  @Benchmark
  public void testTravel() throws IOException {
    passFile("travel.json");
  }

  @Benchmark
  public void testCensusBoundaries() throws IOException {
    passFile("census-boundaries.json");
  }

  @Benchmark
  public void testAnimalServices() throws IOException {
    passFile("animal-services.json");
  }

  @Benchmark
  public void testCrashes() throws IOException {
    passFile("crashes.json");
  }

  @Benchmark
  public void testIncidents() throws IOException {
    passFile("incidents.json");
  }
}
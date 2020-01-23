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

public class JsonReaderITest extends AbstractTest {
  @Test
  public void testTweets() throws IOException {
    passFile("tweets.json");
  }

  @Test
  public void testPaypal() throws IOException {
    passFile("paypal.json");
  }

  @Test
  public void testWebapp() throws IOException {
    passFile("webapp.json");
  }

  @Test
  public void testVatrates() throws IOException {
    passFile("vatrates.json");
  }

  @Test
  public void testEmployees() throws IOException {
    passFile("employees.json");
  }

  @Test
  public void testEarthquakes() throws IOException {
    passFile("earthquakes.json");
  }

  @Test
  public void testDiabetes() throws IOException {
    passFile("diabetes.json");
  }

  @Test
  public void testDcat() throws IOException {
    passFile("dcat.json");
  }

  @Test
  public void testShowtimes() throws IOException {
    passFile("showtimes.json");
  }

  @Test
  public void testCrime() throws IOException {
    passFile("crime.json");
  }

  @Test
  public void testSuicide() throws IOException {
    passFile("suicide.json");
  }

  @Test
  public void testSample() throws IOException {
    passFile("sample.json");
  }

  @Test
  public void testInsurers() throws IOException {
    passFile("insurers.json");
  }

  @Test
  public void testHgbp() throws IOException {
    passFile("hgbp.json");
  }

  @Test
  public void testGithub() throws IOException {
    passFile("github.json");
  }

  @Test
  public void testCensusDiversity() throws IOException {
    passFile("census-diversity.json");
  }

  @Test
  public void testReddit() throws IOException {
    passFile("reddit.json");
  }

  @Test
  public void testSat() throws IOException {
    passFile("sat.json");
  }

  @Test
  public void testGiphy() throws IOException {
    passFile("giphy.json");
  }

  @Test
  public void testJobs() throws IOException {
    passFile("jobs.json");
  }

  @Test
  public void testPowerball() throws IOException {
    passFile("powerball.json");
  }

  @Test
  public void testDemographics() throws IOException {
    passFile("demographics.json");
  }

  @Test
  public void testComplaints() throws IOException {
    passFile("complaints.json");
  }

  @Test
  public void testGraduation() throws IOException {
    passFile("graduation.json");
  }

  @Test
  public void testNobel() throws IOException {
    passFile("nobel.json");
  }

  @Test
  public void testAstronauts() throws IOException {
    passFile("astronauts.json");
  }

  @Test
  public void testMega() throws IOException {
    passFile("mega.json");
  }

  @Test
  public void testMarijuana() throws IOException {
    passFile("marijuana.json");
  }

  @Test
  public void testPets() throws IOException {
    passFile("pets.json");
  }

  @Test
  public void testRestaurants() throws IOException {
    passFile("restaurants.json");
  }

  @Test
  public void testOpportunityZones() throws IOException {
    passFile("opportunity_zones.json");
  }

  @Test
  public void testSalesTax() throws IOException {
    passFile("sales_tax.json");
  }

  @Test
  public void testZipcodes() throws IOException {
    passFile("zipcodes.json");
  }

  @Test
  public void testDatasets() throws IOException {
    passFile("datasets.json");
  }

  @Test
  public void testNames() throws IOException {
    passFile("names.json");
  }

  @Test
  public void testNeighborhoods() throws IOException {
    passFile("neighborhoods.json");
  }

  @Test
  public void testCensusTracts() throws IOException {
    passFile("census-tracts.json");
  }

  @Test
  public void testWeather() throws IOException {
    passFile("weather.json");
  }

  @Test
  public void testSalaries() throws IOException {
    passFile("salaries.json");
  }

  @Test
  public void testMovies() throws IOException {
    passFile("movies.json");
  }
}
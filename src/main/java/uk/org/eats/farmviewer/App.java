package uk.org.eats.farmviewer;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import uk.org.eats.graphdb.CheckAndSetUpGraphDB;

@SpringBootApplication
public class App {
    public static void main(String[] args) throws ClientProtocolException, IOException {
        SpringApplication.run(App.class, args);
        CheckAndSetUpGraphDB.checkRepositorySetUp();
    }
}
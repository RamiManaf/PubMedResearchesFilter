/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rami.prf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.controlsfx.control.Rating;

/**
 *
 * @author Rami Manaf Abdullah
 */
public class MainWindow extends Application implements Initializable {

    private File data;
    private List<String[]> pmids;
    private SimpleIntegerProperty index = new SimpleIntegerProperty(-1);
    private Stage stage;
    @FXML
    private VBox main;
    private VBox loading;
    private Article article;

    private ProgressIndicator indicator;
    private Label loadingText;
    @FXML
    private Label title;
    @FXML
    private Label journal;
    @FXML
    private Label date;
    @FXML
    private Label authors;
    @FXML
    private TextField pmid;
    @FXML
    private TextField doi;
    @FXML
    private Label total;
    @FXML
    private TextArea abstractText;
    @FXML
    private TextField indexField;
    @FXML
    private Button next;
    @FXML
    private Button previous;
    @FXML
    private VBox descriptionBox;
    @FXML
    private Rating rate;
    @FXML
    private CheckBox clinical;
    @FXML
    private TextField numberOfPatients;
    @FXML
    private TextField phase;
    @FXML
    private TextArea findings;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.stage = primaryStage;
        VBox box = new VBox();
        box.setPrefSize(678, 637);
        box.setAlignment(Pos.CENTER);
        box.setSpacing(20);
        indicator = new ProgressIndicator();
        indicator.setPrefSize(200, 200);
        loadingText = new Label("Loading");
        box.getChildren().addAll(indicator, loadingText);
        loading = box;
        stage.setScene(new Scene(loading));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/rami/prf/Main.fxml"));
        loader.setController(this);
        loader.load();
        stage.setOnCloseRequest((t) -> {
            saveChanges();
        });
        stage.setTitle("PubMed Researches Filter");
        stage.show();
    }

    private void setLoading(boolean loading) {
        if (loading) {
            stage.getScene().setRoot(this.loading);
        } else {
            stage.getScene().setRoot(main);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        data = new File("pmids.csv");
        if (!data.exists()) {
            new Alert(Alert.AlertType.ERROR, "Didn't find pmids.csv file", ButtonType.OK).showAndWait();
            System.exit(0);
        }
        try {
            pmids = Utils.loadCSV(data);
        } catch (IOException | SecurityException ex) {
            new Alert(Alert.AlertType.ERROR, "Unable to read pmids.csv file. Please check if the file is accessable.", ButtonType.OK).showAndWait();
            ex.printStackTrace();
            System.exit(0);
        } catch (MalformedDataException ex) {
            new Alert(Alert.AlertType.ERROR, "The CSV file is malformed", ButtonType.OK).showAndWait();
            ex.printStackTrace();
            System.exit(0);
        }
        total.setText(String.valueOf(pmids.size() - 1));
        index.addListener((observable, oldValue, newValue) -> {
            indexField.setText(String.valueOf(index.get() + 1));
            previous.setDisable(index.get() == 0);
            next.setDisable(index.get() == pmids.size() - 2);
            fetch();
        });
        int newIndex = 0;
        for (int i = 1; i < pmids.size(); i++) {
            String value = pmids.get(i)[1];
            if (value != null && !value.isEmpty()) {
                newIndex = i;
            }
        }
        index.set(newIndex == 0 ? 0 : (newIndex - 1));
        main.getChildren().remove(descriptionBox);
        rate.ratingProperty().addListener((ov, t, t1) -> {
            if (rate.getRating() > 1 && !main.getChildren().contains(descriptionBox)) {
                main.getChildren().add(main.getChildren().size() - 1, descriptionBox);
            } else if (rate.getRating() <= 1 && main.getChildren().contains(descriptionBox)) {
                main.getChildren().remove(descriptionBox);
            }
        });
        clinical.selectedProperty().addListener((ov, t, t1) -> {
            phase.setDisable(!clinical.isSelected());
            numberOfPatients.setDisable(!clinical.isSelected());
            if (clinical.isSelected()) {
                phase.setText("");
                numberOfPatients.setText("");
            }
        });
    }

    @FXML
    private void previous() {
        saveChanges();
        index.set(index.get() - 1);
    }

    @FXML
    private void next() {
        saveChanges();
        index.set(index.get() + 1);
    }

    @FXML
    private void navigate() {
        saveChanges();
        fetch();
    }

    @FXML
    private void openPaper() {
        getHostServices().showDocument("https://sci-hub.se/" + article.getDoi());
    }

    private void saveChanges() {
        String[] values = pmids.get(index.get() + 1);
        values[1] = String.valueOf((int) rate.getRating());
        values[2] = String.valueOf(clinical.isSelected());
        values[3] = phase.getText();
        values[4] = numberOfPatients.getText();
        values[5] = findings.getText();
        try {
            Utils.saveCSV(pmids, data);
        } catch (IOException ex) {
            ex.printStackTrace();
            boolean done = false;
            ButtonType saveInOtherPlace = new ButtonType("Save in other place");
            while (!done) {
                Optional<ButtonType> result = new Alert(Alert.AlertType.ERROR, "Couldn't save data. Make sure this program has the correct accessiblities.", saveInOtherPlace, ButtonType.CLOSE).showAndWait();
                if (result.isPresent() && result.equals(saveInOtherPlace)) {
                    FileChooser chooser = new FileChooser();
                    chooser.setInitialFileName("pmids");
                    chooser.setTitle("Choose where to save the file");
                    File choosen = chooser.showSaveDialog(stage);
                    if (choosen != null) {
                        try {
                            Utils.saveCSV(pmids, choosen);
                            done = true;
                        } catch (IOException ex1) {
                            ex.printStackTrace();
                        }
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
        }
    }

    private void fetch() {
        int newIndex;
        try {
            newIndex = Integer.parseInt(indexField.getText()) - 1;
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "Please enter a valid number", ButtonType.OK).show();
            indexField.setText(String.valueOf(index.get() + 1));
            return;
        }
        if (newIndex < 0 || newIndex >= pmids.size() - 1) {
            new Alert(Alert.AlertType.ERROR, "Please enter a value between 1 and " + total.getText(), ButtonType.OK).showAndWait();
            indexField.setText(String.valueOf(index.get() + 1));
            return;
        }
        setLoading(true);
        new FetchPubMed(pmids.get(newIndex + 1)).start();
    }

    private class FetchPubMed extends Thread {

        private XMLStreamReader reader;
        private String id;
        private String[] values;
        private String error;

        public FetchPubMed(String[] values) {
            this.id = values[0];
            this.values = values;
        }

        @Override
        public void run() {
            error = null;
            try {
                Platform.runLater(() -> loadingText.setText("Downloading data from PubMed..."));
                ByteArrayInputStream in = downloadUrl(new URL("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=" + id + "&rettype=xml"));
                Platform.runLater(() -> loadingText.setText("Parsing data..."));
                reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
                try {
                    boolean openDate = false;
                    boolean openAbstract = false, openTitle = false;
                    while (reader.hasNext()) {
                        reader.next();
                        if (reader.isStartElement()) {
                            switch (reader.getName().getLocalPart()) {
                                case "PubmedArticle": {
                                    article = new Article();
                                    break;
                                }
                                case "ArticleId": {
                                    String type = reader.getAttributeValue(null, "IdType");
                                    if (type.equals("doi")) {
                                        article.setDoi(reader.getElementText());
                                    } else if (type.equals("pubmed")) {
                                        article.setPmid(reader.getElementText());
                                    }
                                    break;
                                }
                                case "Title": {
                                    article.setJournalTitle(reader.getElementText());
                                    break;
                                }
                                case "Volume": {
                                    article.setJournalVloume(reader.getElementText());
                                    break;
                                }
                                case "Issue": {
                                    article.setJournalIssue(reader.getElementText());
                                    break;
                                }
                                case "ArticleTitle": {
                                    openTitle = true;
                                    break;
                                }
                                case "AbstractText": {
                                    if (reader.getAttributeValue(null, "Label") != null) {
                                        article.appendAbstractText("\n" + reader.getAttributeValue(null, "Label") + "\n");
                                    }
                                    openAbstract = true;
                                    break;
                                }
                                case "ArticleDate": {
                                    openDate = true;
                                    break;
                                }
                                case "Year": {
                                    if (openDate) {
                                        article.setDate(reader.getElementText());
                                    }
                                    break;
                                }
                                case "Month": {
                                    if (openDate) {
                                        article.setDate(article.getDate() + "/" + reader.getElementText());
                                    }
                                    break;
                                }
                                case "Day": {
                                    if (openDate) {
                                        article.setDate(article.getDate() + "/" + reader.getElementText());
                                    }
                                    break;
                                }
                                case "Author": {
                                    article.getAuthors().add("");
                                    break;
                                }
                                case "ForeName": {
                                    int last = article.getAuthors().size() - 1;
                                    article.getAuthors().set(last, reader.getElementText() + article.getAuthors().get(last));
                                    break;
                                }
                                case "LastName": {
                                    int last = article.getAuthors().size() - 1;
                                    article.getAuthors().set(last, article.getAuthors().get(last) + reader.getElementText());
                                    break;
                                }
                            }
                        } else if (reader.isEndElement()) {
                            switch (reader.getName().getLocalPart()) {
                                case "ArticleDate": {
                                    openDate = false;
                                    break;
                                }
                                case "AbstractText": {
                                    openAbstract = false;
                                    break;
                                }
                                case "ArticleTitle": {
                                    openTitle = false;
                                    break;
                                }
                            }
                        } else if (reader.isCharacters()) {
                            if (openAbstract) {
                                article.appendAbstractText(reader.getText());
                            } else if (openTitle) {
                                article.setTitle(article.getTitle() + reader.getText());
                            }
                        }
                    }
                } catch (XMLStreamException ex) {
                    error = "An error happend while parsing data from PubMed. The data will not correctly be viewed";
                    ex.printStackTrace();
                }
            } catch (XMLStreamException ex) {
                error = "Cann't parse NCBI data. Please contact Rami Manaf about this.";
                ex.printStackTrace();
            } catch (MalformedURLException ex) {
                error = "PMID: " + id + " is malformed.";
                ex.printStackTrace();
            } catch (IOException ex) {
                error = "Please check your internet connection and try again.";
                ex.printStackTrace();
            }
            Platform.runLater(() -> {
                if (FetchPubMed.this.error != null) {
                    new Alert(Alert.AlertType.ERROR, FetchPubMed.this.error, ButtonType.OK).showAndWait();
                    MainWindow.this.indexField.setText(String.valueOf(MainWindow.this.index.get() + 1));
                } else {
                    title.setText(article.getTitle());
                    journal.setText(article.getJournalTitle() + ":" + article.getJournalVloume() + ":" + article.getJournalIssue());
                    date.setText(article.getDate());
                    date.setText(article.getDate());
                    doi.setText(article.getDoi());
                    pmid.setText(id);
                    abstractText.setText(article.getAbstractText());
                    authors.setText(String.join(", ", article.getAuthors()));
                    rate.setRating(values[1] == null || values[1].isEmpty() ? 1 : Integer.parseInt(values[1]));
                    clinical.setSelected(values[2] == null || values[2].isEmpty() ? false : Boolean.parseBoolean(values[2]));
                    phase.setText(Objects.toString(values[3], ""));
                    numberOfPatients.setText(Objects.toString(values[4], ""));
                    findings.setText(Objects.toString(values[5], ""));
                }
                MainWindow.this.index.set(Integer.parseInt(indexField.getText()) - 1);
                setLoading(false);
            });
        }
    }

    private ByteArrayInputStream downloadUrl(URL toDownload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        InputStream stream = toDownload.openStream();
        while ((bytesRead = stream.read(chunk)) > 0) {
            outputStream.write(chunk, 0, bytesRead);
        }
        String x = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        x = x.substring(x.indexOf('\n') + 1);
        x = x.substring(x.indexOf('\n') + 1);
        return new ByteArrayInputStream(x.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}

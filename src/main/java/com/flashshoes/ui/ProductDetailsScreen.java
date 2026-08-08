package com.flashshoes.ui;

import com.flashshoes.model.*;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.util.List;

/**
 * The page you land on after tapping a product card in the catalogue.
 * Shows the big image, description, lets the customer pick a size and
 * quantity, shows (fake) reviews, and adds the configured item to the cart.
 */
public class ProductDetailsScreen extends BorderPane {

    private final DataStore store = DataStore.getInstance();
    private final Product product;
    private final Customer customer;
    private final Runnable onBack;

    private String selectedSize;
    private int quantity = 1;
    private Label qtyLabel;

    public ProductDetailsScreen(Product product, Runnable onBack) {
        this.product = product;
        this.customer = (Customer) store.getCurrentUser();
        this.onBack = onBack;

        setStyle("-fx-background-color: #F7F8FA;");
        setTop(buildTopBar());
        setCenter(buildCenter());
    }

    private Node buildTopBar() {
        HBox bar = new HBox(16);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Catalogue");
        backBtn.getStyleClass().add("btn-ghost");
        backBtn.setOnAction(e -> onBack.run());

        Label title = new Label("👟 FlashShoes");
        title.getStyleClass().add("top-bar-title");

        bar.getChildren().addAll(backBtn, title);
        return bar;
    }

    private Node buildCenter() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        VBox root = new VBox(24);
        root.setPadding(new Insets(24));
        root.setMaxWidth(900);

        // ---- top: image + info side by side ----
        HBox top = new HBox(28);

        StackPane imagePane = new StackPane();
        imagePane.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E5E7EB; -fx-border-radius: 12;");
        imagePane.setPrefSize(340, 340);
        ImageView imageView = new ImageView(loadImage(product.getImagePath()));
        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);
        imagePane.getChildren().add(imageView);

        VBox info = new VBox(10);
        info.setPrefWidth(480);

        FlashSale sale = findActiveSaleFor(product);

        Label brand = new Label(product.getBrand().toUpperCase());
        brand.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF; -fx-font-weight: bold;");

        Label name = new Label(product.getName());
        name.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        name.setWrapText(true);

        HBox priceRow = new HBox(10);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        if (sale != null) {
            Label discounted = new Label(String.format("$%.2f", sale.getDiscountedPrice()));
            discounted.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
            Label was = new Label(String.format("$%.2f", product.getPrice()));
            was.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF; -fx-strikethrough: true;");
            Label badge = new Label(String.format("%.0f%% OFF · %d left", sale.getDiscountPercent(), sale.getLimitedStock()));
            badge.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 3 8 3 8;");
            priceRow.getChildren().addAll(discounted, was, badge);
        } else {
            Label price = new Label(String.format("$%.2f", product.getPrice()));
            price.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            priceRow.getChildren().add(price);
        }

        Label category = new Label(product.getCategory() + " · " + product.getGender());
        category.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        Label stock = new Label(product.getStockQuantity() > 0
                ? product.getStockQuantity() + " in stock" : "Out of stock");
        stock.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (product.getStockQuantity() > 0 ? "#059669" : "#DC2626") + ";");

        Label descHeader = new Label("Description");
        descHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        Label desc = new Label(product.getDescription() == null || product.getDescription().isBlank()
                ? "A great pick from " + product.getBrand() + " — comfortable, durable, and made for everyday wear."
                : product.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #4B5563;");

        // ---- size selector ----
        Label sizeHeader = new Label("Select Size");
        sizeHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        FlowPane sizeRow = new FlowPane(8, 8);
        ToggleGroup sizeGroup = new ToggleGroup();
        List<String> sizes = product.getSizes();
        if (sizes == null || sizes.isEmpty()) sizes = List.of("Standard");
        for (String size : sizes) {
            ToggleButton tb = new ToggleButton(size);
            tb.setToggleGroup(sizeGroup);
            tb.getStyleClass().add("btn-outline");
            tb.setOnAction(e -> selectedSize = size);
            sizeRow.getChildren().add(tb);
        }
        // default-select the first size
        if (!sizeGroup.getToggles().isEmpty()) {
            sizeGroup.selectToggle(sizeGroup.getToggles().get(0));
            selectedSize = sizes.get(0);
        }

        // ---- quantity stepper ----
        Label qtyHeader = new Label("Quantity");
        qtyHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        HBox qtyRow = new HBox(10);
        qtyRow.setAlignment(Pos.CENTER_LEFT);
        Button minusBtn = new Button("−");
        Button plusBtn = new Button("+");
        minusBtn.getStyleClass().add("btn-outline");
        plusBtn.getStyleClass().add("btn-outline");
        qtyLabel = new Label(String.valueOf(quantity));
        qtyLabel.setStyle("-fx-font-size: 14px; -fx-padding: 0 12 0 12;");
        minusBtn.setOnAction(e -> {
            if (quantity > 1) quantity--;
            qtyLabel.setText(String.valueOf(quantity));
        });
        plusBtn.setOnAction(e -> {
            if (quantity < product.getStockQuantity()) quantity++;
            qtyLabel.setText(String.valueOf(quantity));
        });
        qtyRow.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        Button addBtn = new Button(sale != null ? "⚡ Add to Cart (Flash Sale)" : "Add to Cart");
        addBtn.getStyleClass().add(sale != null ? "btn-danger" : "btn-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setDisable(product.getStockQuantity() <= 0);
        addBtn.setOnAction(e -> {
            customer.addToCart(product, quantity, selectedSize);
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    quantity + " × " + product.getName() + " (size " + selectedSize + ") added to your cart.");
            alert.setHeaderText(null);
            alert.showAndWait();
        });

        info.getChildren().addAll(brand, name, priceRow, category, stock, new Separator(),
                descHeader, desc, sizeHeader, sizeRow, qtyHeader, qtyRow, addBtn);

        top.getChildren().addAll(imagePane, info);

        // ---- reviews ----
        VBox reviewsSection = buildReviewsSection();

        root.getChildren().addAll(top, new Separator(), reviewsSection);
        scroll.setContent(root);
        return scroll;
    }

    private VBox buildReviewsSection() {
        VBox box = new VBox(12);
        List<Review> reviews = store.getReviewsFor(product.getProductId());

        double avg = reviews.isEmpty() ? 0 :
                reviews.stream().mapToInt(Review::getRating).average().orElse(0);

        Label header = new Label(reviews.isEmpty()
                ? "Reviews"
                : String.format("Reviews (%d) · %.1f★ average", reviews.size(), avg));
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        box.getChildren().add(header);

        if (reviews.isEmpty()) {
            Label none = new Label("No reviews yet — be the first to try this one!");
            none.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");
            box.getChildren().add(none);
        } else {
            for (Review r : reviews) {
                VBox card = new VBox(4);
                card.setPadding(new Insets(12));
                card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");

                HBox line1 = new HBox(8);
                line1.setAlignment(Pos.CENTER_LEFT);
                Label stars = new Label(r.getStars());
                stars.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 13px;");
                Label reviewer = new Label(r.getReviewerName());
                reviewer.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                Label date = new Label(r.getDate().toString());
                date.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
                line1.getChildren().addAll(stars, reviewer, date);

                Label comment = new Label(r.getComment());
                comment.setWrapText(true);
                comment.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #4B5563;");

                card.getChildren().addAll(line1, comment);
                box.getChildren().add(card);
            }
        }
        return box;
    }

    private FlashSale findActiveSaleFor(Product product) {
        for (FlashSale sale : store.getActiveFlashSales()) {
            if (sale.getProduct().getProductId().equals(product.getProductId())) return sale;
        }
        return null;
    }

    private Image loadImage(String path) {
        File f = new File(path);
        if (f.exists()) return new Image(f.toURI().toString());
        return new Image(getClass().getResourceAsStream("/placeholder.png") == null
                ? "https://via.placeholder.com/300x300?text=Product" : "/placeholder.png");
    }
}

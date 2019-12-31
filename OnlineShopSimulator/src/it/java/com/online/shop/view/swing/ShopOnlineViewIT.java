package com.online.shop.view.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.Arrays;

import javax.swing.DefaultListModel;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.online.shop.controller.CartController;
import com.online.shop.controller.ShopController;
import com.online.shop.model.Cart;
import com.online.shop.model.Item;
import com.online.shop.repository.mongo.ItemsMongoRepository;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

public class ShopOnlineViewIT extends AssertJSwingJUnitTestCase {


	private static final String CART_FIXTURE_LABEL_TEST = "cartTest";
	private static final String ITEM_FIXTURE_NAME_2 = "test1";
	private static final String ITEM_FIXTURE_PRODUCTCODE_2 = "2";
	private static final String ITEM_FIXTURE_NAME_1 = "test";
	private static final String ITEM_FIXTURE_PRODUCTCODE_1 = "1";
	private static final String CART_FIXTURE_LABEL_1 = "test";
	private static final int ITEM_FIXTURE_NEW_QUANTITY = 1;
	private static final int ITEM_FIXTURE_QUANTITY_1 = 10;
	private static final int MODIFIER = 1;
	private static final int HEIGHT = 600;
	private static final int WIDTH = 676;
	private static final int FIRST_ITEM = 0;
	private static final int SECOND_ITEM = 1;

	@SuppressWarnings("rawtypes")
	@ClassRule
	public static final GenericContainer mongo = new GenericContainer("mongo:4.0.5").withExposedPorts(27017);

	private static MongoServer server;	
	private MongoClient mongoClient;
	private FrameFixture window;
	private static InetSocketAddress serverAddress;

	private ShopController shopController;
	private CartController cartController;
	private ItemsMongoRepository itemsRepository;
	private ShopOnlineView shopOnlineView;

	@BeforeClass
	public static void setupServer() {
		server = new MongoServer(new MemoryBackend());
		serverAddress = server.bind();
	}

	@AfterClass
	public static void shutdownServer() {
		server.shutdown();
	}

	@Override
	protected void onSetUp() {
		mongoClient = new MongoClient(new ServerAddress(serverAddress));
		itemsRepository = new ItemsMongoRepository(mongoClient);
		for(Item item : itemsRepository.findAll()) {
			itemsRepository.remove(item.getProductCode());
		}
		for(Cart cart: itemsRepository.findAllCarts()) {
			itemsRepository.removeCart(cart.getDate(),cart.getLabel());

		}
		GuiActionRunner.execute(
				()->{
					shopOnlineView = new ShopOnlineView();
					shopController = new ShopController(shopOnlineView,itemsRepository);
					cartController = new CartController(shopOnlineView,itemsRepository,shopOnlineView);
					shopOnlineView.setCartController(cartController);
					shopOnlineView.setShopController(shopController);
					return shopOnlineView;
				});
		window = new FrameFixture(robot(),shopOnlineView);
		Dimension dimension = new Dimension(WIDTH, HEIGHT);
		window.show(dimension);
	}		

	@Override
	protected void onTearDown() {
		mongoClient.close();
	}

	@Test @GUITest
	public void testAllItems() {
		Item itemExist1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1);
		Item itemExist2 = new Item(ITEM_FIXTURE_PRODUCTCODE_2,ITEM_FIXTURE_NAME_2);
		itemsRepository.store(itemExist1);
		itemsRepository.store(itemExist2);
		GuiActionRunner.execute(
				()->{
					shopController.allItems();
				});
		assertThat(window.list("itemListShop").contents()).containsExactly(itemExist1.toString(),itemExist2.toString());
	}
	@Test @GUITest
	public void testAllCarts() {
		Item itemExist1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1);
		Item itemExist2 = new Item(ITEM_FIXTURE_PRODUCTCODE_2,ITEM_FIXTURE_NAME_2);
		Cart cartExist = new Cart(Arrays.asList(itemExist1,itemExist2),CART_FIXTURE_LABEL_1);
		itemsRepository.store(itemExist1);
		itemsRepository.store(itemExist2);
		itemsRepository.storeCart(cartExist);
		GuiActionRunner.execute(
				()-> cartController.allCarts()
				);
		assertThat(window.list("listCart").contents()).containsExactly(cartExist.toString());
	}
	@Test @GUITest
	public void testAddButtonSuccess() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_QUANTITY_1);
		GuiActionRunner.execute(
				()-> {
					shopController.newItem(item1);
				});
		window.list("itemListShop").selectItem(0);
		window.button(JButtonMatcher.withText("Add")).click();
		assertThat(window.list("itemListCart").contents()).containsExactly(
				new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_NEW_QUANTITY).toString());
	}

	@Test @GUITest
	public void testAddButtonSuccessModifyQuantity() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_QUANTITY_1);
		GuiActionRunner.execute(
				()-> {
					shopController.newItem(item1);
					cartController.addToCart(item1);
				});
		window.list("itemListShop").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Add")).click();
		assertThat(window.list("itemListCart").contents()).containsExactly(
				new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_NEW_QUANTITY+MODIFIER).toString());
	}

	@Test @GUITest
	public void testAddButtonError() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_QUANTITY_1);
		itemsRepository.store(item1);
		GuiActionRunner.execute(
				()-> {
					shopController.allItems();
					cartController.addToCart(item1);
				});
		window.list("itemListShop").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Add")).click();
		assertThat(window.list("itemListCart").contents()).containsExactly(
				new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_QUANTITY_1).toString());
		JButtonFixture addButton = window.button(JButtonMatcher.withText("Add"));	
		window.list("itemListShop").clearSelection();
		addButton.requireDisabled();
	}

	@Test @GUITest
	public void testRemoveButtonSuccess() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_NEW_QUANTITY);
		GuiActionRunner.execute(
				()-> {
					cartController.addToCart(item1);
				});
		window.list("itemListCart").selectItem(0);
		window.button(JButtonMatcher.withText("Remove")).click();
		assertThat(window.list("itemListCart").contents()).isEmpty();
	}

	@Test @GUITest
	public void testRemoveButtonSuccessModifyQuantity() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_NEW_QUANTITY+MODIFIER);
		GuiActionRunner.execute(
				()-> {
					shopController.newItem(item1);
					cartController.addToCart(item1);
				});
		window.list("itemListShop").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Add")).click();
		window.list("itemListCart").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Remove")).click();
		assertThat(window.list("itemListCart").contents()).containsExactly(
				new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_NEW_QUANTITY).toString());
	}

	@Test @GUITest
	public void testBuyButtonSuccess() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_QUANTITY_1);
		GuiActionRunner.execute(
				()-> {
					shopController.newItem(item1);
					cartController.addToCart(item1);
				});
		window.textBox("cartNameText").enterText(CART_FIXTURE_LABEL_TEST);
		window.list("itemListCart").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Buy")).click();
		assertThat(window.list("itemListCart").contents()).isEmpty();
		assertThat(window.list("itemListShop").contents()).containsExactly(
				new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_QUANTITY_1-MODIFIER).toString());
		assertThat(window.list("listCart").contents()).containsExactly(
				new Cart(CART_FIXTURE_LABEL_TEST,LocalDate.now().toString()).toString());
		window.list("listCart").selectItem(0);
		assertThat(window.list("listItemsCart").contents()).containsExactly(
				new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_NEW_QUANTITY).toString());
	}
	
	@Test @GUITest
	public void testBuyButtonError() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1,ITEM_FIXTURE_QUANTITY_1);
		GuiActionRunner.execute(
				()->{ 
					DefaultListModel<Item> items = shopOnlineView.getItemListCartModel();
					items.addElement(item1);
				});
		window.textBox("cartNameText").enterText(CART_FIXTURE_LABEL_TEST);
		window.list("itemListCart").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Buy")).click();
		assertThat(window.list("itemListCart").contents()).containsExactly(item1.toString());
	}
	
	@Test @GUITest
	public void testAllCartItems() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1);
		Item item2 = new Item(ITEM_FIXTURE_PRODUCTCODE_2,ITEM_FIXTURE_NAME_2);
		Cart cart = new Cart(Arrays.asList(item1,item2),CART_FIXTURE_LABEL_1);
		itemsRepository.store(item1);
		itemsRepository.store(item2);
		itemsRepository.storeCart(cart);
		GuiActionRunner.execute(
				()-> cartController.allCarts()
				);
		window.list("listCart").selectItem(FIRST_ITEM);
		GuiActionRunner.execute(
				()-> cartController.allItemsCart(cart)
				);
		assertThat(window.list("listItemsCart").contents()).containsExactly(cart.getItems().get(FIRST_ITEM).toString(),
				cart.getItems().get(SECOND_ITEM).toString());
	}

	@Test @GUITest
	public void testDeleteButtonSuccess() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1);
		Item item2 = new Item(ITEM_FIXTURE_PRODUCTCODE_2,ITEM_FIXTURE_NAME_2);
		Cart cart = new Cart(Arrays.asList(item1,item2),CART_FIXTURE_LABEL_1);
		itemsRepository.store(item1);
		itemsRepository.store(item2);
		itemsRepository.storeCart(cart);
		GuiActionRunner.execute(
				()-> cartController.allCarts()		
				);
		window.list("listCart").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Delete")).click();
		assertThat(window.list("listCart").contents()).isEmpty();
	}

	@Test @GUITest
	public void testRemoveButtonError() {
		Item item1 = new Item(ITEM_FIXTURE_PRODUCTCODE_1,ITEM_FIXTURE_NAME_1);
		Item item2 = new Item(ITEM_FIXTURE_PRODUCTCODE_2,ITEM_FIXTURE_NAME_2);
		Cart cart = new Cart(Arrays.asList(item1,item2),CART_FIXTURE_LABEL_1);
		GuiActionRunner.execute(
				()->{ 
					DefaultListModel<Cart> carts = shopOnlineView.getListCartModel();
					carts.addElement(cart);
				});
		window.list("listCart").selectItem(FIRST_ITEM);
		window.button(JButtonMatcher.withText("Delete")).click();
		assertThat(window.list("listCart").contents()).containsExactly(cart.toString());
	}	

}
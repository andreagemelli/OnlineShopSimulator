package com.online.shop.controller;

import com.online.shop.model.Item;
import com.online.shop.repository.ItemsRepository;
import com.online.shop.view.ItemsView;

public class ShopController {

	private ItemsView itemsView;
	private ItemsRepository itemsRepository;

	public ShopController(ItemsView itemsView, ItemsRepository itemsRepository) {
		this.itemsView = itemsView;
		this.itemsRepository = itemsRepository;
	}

	public void allItems() {
		itemsView.showItems(itemsRepository.findAll());
	}

	public void newItem(Item item) {
		Item retrievedItem = itemsRepository.findByProductCode(item.getProductCode());

		if (item.getQuantity() <= 0) {
			throw new IllegalArgumentException("Negative amount: " + item.getQuantity());
		}

		if (retrievedItem != null) {
			itemsRepository.increaseQuantity(item);
			itemsView.itemQuantityAdded(retrievedItem);
			return;
		}

		itemsRepository.store(item);
		itemsView.itemAdded(item);
	}

	public void removeItem(Item item) {

		if (itemsRepository.findByProductCode(item.getProductCode()) == null) {
			itemsView.errorLog("Item with production code " + item.getProductCode() + " does not exists", item);
			return;
		}
		itemsRepository.remove(item);
		itemsView.itemRemoved(item);
	}

	public void searchItem(Item item) {
		Item retrievedItem = itemsRepository.findByName(item.getName());

		if (retrievedItem == null) {
			itemsView.errorLog("Item with name " + item.getName() + " doest not exists", item);
			return;
		}

		itemsView.showSearchResult(item);
	}

}

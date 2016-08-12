package com.gh.helper.domain

import java.util.Date

/**
 * Customers search parameters.
 *
 * @param title 		 name
 * @param store			 name of store
 * @param details 	 details
 */
case class GroceryListSearchParameters(title: Option[String] = None,
		                                    store: Option[String] = None,
		                                    details: Option[String] = None)
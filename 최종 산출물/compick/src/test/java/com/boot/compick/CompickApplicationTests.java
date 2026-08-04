package com.boot.compick;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CompickApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void homePageRenders() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("내게 맞는 PC를 더 쉽게")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("product-detail-dialog")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("/js/home.js")
			));
	}

	@Test
	@Sql(statements = {
		"INSERT INTO CATEGORY (category_name) VALUES ('CPU')",
		"""
		INSERT INTO PRODUCT (
			category_id, product_name, brand, model_name, price,
			rating_count, stock_quantity, image_url, sales_status, created_at
		) VALUES (
			(SELECT category_id FROM CATEGORY WHERE category_name = 'CPU'),
			'통합 테스트 인기 CPU', 'COMPICK', 'POPULAR-CPU',
			729000, 500, 10, 'https://example.com/popular-cpu.jpg',
			'ON_SALE', TIMESTAMP '2026-07-29 10:00:00'
		);
		""",
		"""
		INSERT INTO PRODUCT (
			category_id, product_name, brand, model_name, price,
			rating_count, stock_quantity, sales_status, created_at
		) VALUES (
			(SELECT category_id FROM CATEGORY WHERE category_name = 'CPU'),
			'통합 테스트 일반 CPU', 'COMPICK', 'NORMAL-CPU',
			329000, 100, 5, 'ON_SALE', TIMESTAMP '2026-07-29 09:00:00'
		);
		""",
		"""
		INSERT INTO PRODUCT (
			category_id, product_name, brand, model_name, price,
			rating_count, stock_quantity, sales_status, created_at
		) VALUES (
			(SELECT category_id FROM CATEGORY WHERE category_name = 'CPU'),
			'재고 없는 제외 상품', 'COMPICK', 'SOLD-OUT-CPU',
			999000, 999, 0, 'ON_SALE', TIMESTAMP '2026-07-29 11:00:00'
		);
		"""
	})
	@Sql(
		statements = {
			"DELETE FROM PRODUCT",
			"DELETE FROM CATEGORY"
		},
		executionPhase = ExecutionPhase.AFTER_TEST_METHOD
	)
	void homePageRendersPopularProductsFromDatabase() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("통합 테스트 인기 CPU")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("통합 테스트 일반 CPU")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("729,000원")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString(
					"src=\"https://example.com/popular-cpu.jpg\""
				)
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString(
					"src=\"/images/products/product-placeholder.svg\""
				)
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.not(
					org.hamcrest.Matchers.containsString("재고 없는 제외 상품")
				)
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.not(
					org.hamcrest.Matchers.containsString("인기 상품을 준비 중입니다.")
				)
			));
	}

	@Test
	void homePageProvidesMainNavigationLinks() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/products\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/quotes/new\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/preset\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/ai-quotes\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/cart\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/login\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString(
					"src=\"/images/icons/parts-shopping.svg\""
				)
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString(
					"src=\"/images/icons/quote-shopping.svg\""
				)
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString(
					"src=\"/images/icons/preset-recommendation.svg\""
				)
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString(
					"src=\"/images/icons/ai-recommendation.svg\""
				)
			));
	}

	@Test
	void reusableImageAssetsAreServed() throws Exception {
		String[] imagePaths = {
			"/images/icons/parts-shopping.svg",
			"/images/icons/quote-shopping.svg",
			"/images/icons/preset-recommendation.svg",
			"/images/icons/ai-recommendation.svg",
			"/images/products/product-placeholder.svg"
		};

		for (String imagePath : imagePaths) {
			mockMvc.perform(get(imagePath))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
		}
	}

	@Test
	void cartApiRequiresLogin() throws Exception {
		mockMvc.perform(post("/api/cart/items")
				.with(csrf())
				.contentType("application/json")
				.content("""
					{"productId":1,"quantity":1}
					"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void anonymousHeaderShowsLoginOnly() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/login\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.not(
					org.hamcrest.Matchers.containsString("href=\"/mypage\"")
				)
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.not(
					org.hamcrest.Matchers.containsString("action=\"/logout\"")
				)
			));
	}

	@Test
	void authenticatedHeaderShowsMyPageAndLogout() throws Exception {
		mockMvc.perform(get("/").with(user("user").roles("USER")))
			.andExpect(status().isOk())
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("href=\"/mypage\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.containsString("action=\"/logout\"")
			))
			.andExpect(content().string(
				org.hamcrest.Matchers.not(
					org.hamcrest.Matchers.containsString("href=\"/login\"")
				)
			));
	}

	@Test
	void myPageRedirectsAnonymousUserToLogin() throws Exception {
		mockMvc.perform(get("/mypage"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("http://localhost/login"));
	}
}

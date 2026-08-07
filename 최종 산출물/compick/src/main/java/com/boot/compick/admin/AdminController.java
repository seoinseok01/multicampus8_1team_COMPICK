package com.boot.compick.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberRole;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.repository.AddressRepository;
import com.boot.compick.member.repository.MemberRepository;
import com.boot.compick.order.entity.OrderEntity;
import com.boot.compick.order.entity.OrderStatus;
import com.boot.compick.order.repository.OrderRepository;
import com.boot.compick.payment.repository.PaymentRepository;
import com.boot.compick.product.entity.CategoryEntity;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.CategoryRepository;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.dto.PresetUpsertRequest;
import com.boot.compick.quote.entity.AiRecommendationEntity;
import com.boot.compick.quote.entity.PurposeTag;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.entity.QuoteType;
import com.boot.compick.quote.repository.AiRecommendationRepository;
import com.boot.compick.quote.repository.QuoteRepository;
import com.boot.compick.quote.service.PresetAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@Transactional(readOnly = true)
public class AdminController {
	private static final int ADMIN_PAGE_SIZE = 10;

	private final MemberRepository members;
	private final AddressRepository addresses;
	private final ProductRepository products;
	private final CategoryRepository categories;
	private final QuoteRepository quotes;
	private final OrderRepository orders;
	private final PaymentRepository payments;
	private final AiRecommendationRepository aiRecommendations;
	private final PresetAdminService presetAdminService;
	private final ObjectMapper objectMapper;

	public AdminController(MemberRepository members, AddressRepository addresses,
		ProductRepository products, CategoryRepository categories, QuoteRepository quotes,
		OrderRepository orders, PaymentRepository payments,
		AiRecommendationRepository aiRecommendations, PresetAdminService presetAdminService,
		ObjectMapper objectMapper) {
		this.members = members;
		this.addresses = addresses;
		this.products = products;
		this.categories = categories;
		this.quotes = quotes;
		this.orders = orders;
		this.payments = payments;
		this.aiRecommendations = aiRecommendations;
		this.presetAdminService = presetAdminService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/admin")
	public String dashboard(Model model) {
		LocalDate today = LocalDate.now();
		List<OrderEntity> allOrders = orders.findAllByOrderByOrderedAtDesc();
		List<ProductEntity> allProducts = products.findAll();
		long todaySales = allOrders.stream()
			.filter(o -> o.getOrderedAt().toLocalDate().equals(today))
			.filter(o -> o.getOrderStatus() != OrderStatus.PAYMENT_PENDING && o.getOrderStatus() != OrderStatus.CANCELLED)
			.mapToLong(OrderEntity::getFinalAmount).sum();
		model.addAttribute("todaySales", todaySales);
		model.addAttribute("todayOrders", orders.countByOrderedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
		model.addAttribute("newMembers", members.countByCreatedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
		List<ProductEntity> lowStockProducts = allProducts.stream()
			.filter(product -> product.getStockQuantity() < 5)
			.toList();
		model.addAttribute("lowStockCount", lowStockProducts.size());
		List<AdminOrder> returnRequests = enrichOrders(allOrders.stream()
			.filter(OrderEntity::isReturnRequested).toList());
		model.addAttribute("returnRequestCount", returnRequests.size());
		model.addAttribute("returnRequests", returnRequests.stream().limit(5).toList());
		model.addAttribute("recentOrders", enrichOrders(orders.findTop5ByOrderByOrderedAtDesc()));
		model.addAttribute("lowStockProducts", lowStockProducts.stream().limit(5).toList());
		return "admin/dashboard";
	}

	@GetMapping("/admin/members")
	public String memberList(@RequestParam(defaultValue = "") String keyword,
		@RequestParam(defaultValue = "") String role, @RequestParam(defaultValue = "") String status,
		@RequestParam(defaultValue = "1") int page, Model model) {
		String needle = keyword.toLowerCase(Locale.ROOT).trim();
		List<Member> result = members.findAll().stream()
			.filter(m -> role.isBlank() || m.getRole().name().equals(role))
			.filter(m -> status.isBlank() || m.getStatus().name().equals(status))
			.filter(m -> needle.isBlank() || (m.getLoginId() + " " + m.getName() + " " + m.getEmail()).toLowerCase(Locale.ROOT).contains(needle))
			.sorted(Comparator.comparing(Member::getCreatedAt).reversed()).toList();
		addPage(model, "members", result, page);
		model.addAttribute("keyword", keyword);
		model.addAttribute("role", role);
		model.addAttribute("status", status);
		return "admin/member-list";
	}

	@GetMapping("/admin/members/{id}")
	public String memberDetail(@PathVariable Long id, Model model) {
		Member member = member(id);
		model.addAttribute("member", member);
		model.addAttribute("addresses", addresses.findAllByMemberIdOrderByDefaultYnDescIdDesc(id));
		model.addAttribute("orders", orders.findByMemberIdOrderByOrderedAtDesc(id));
		return "admin/member-detail";
	}

	@PostMapping("/admin/members/{id}")
	@Transactional
	public String updateMember(@PathVariable Long id, @RequestParam MemberRole role,
		@RequestParam MemberStatus status, RedirectAttributes redirect) {
		member(id).updateAdministration(role, status);
		redirect.addFlashAttribute("message", "회원 권한과 상태를 저장했습니다.");
		return "redirect:/admin/members/" + id;
	}

	@GetMapping("/admin/products")
	public String productForm(@RequestParam(required = false) Long id,
		@RequestParam(defaultValue = "") String keyword,
		@RequestParam(defaultValue = "") String category,
		@RequestParam(defaultValue = "register") String tab,
		@RequestParam(defaultValue = "1") int page, Model model) {
		ProductEntity editingProduct = id == null ? null : product(id);
		model.addAttribute("product", editingProduct);
		String needle = keyword.toLowerCase(Locale.ROOT).trim();
		List<ProductEntity> result = products.findAll().stream()
			.filter(p -> category.isBlank() || p.getCategory().getCategoryName().equals(category))
			.filter(p -> needle.isBlank() || (p.getProductName() + " " + p.getBrand() + " " + p.getModelName())
				.toLowerCase(Locale.ROOT).contains(needle))
			.sorted(Comparator.comparing(ProductEntity::getProductId).reversed()).toList();
		addPage(model, "products", result, page);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedCategory", category);
		model.addAttribute("activeTab", id == null && "list".equals(tab) ? "list" : "register");
		model.addAttribute("specsJson", editingProduct == null || editingProduct.getSpecJson() == null ? "{}" : editingProduct.getSpecJson());
		model.addAttribute("categories", categories.findAll());
		return "admin/product-form";
	}

	@PostMapping("/admin/products")
	@Transactional
	public String saveProduct(@RequestParam(required = false) Long id, @RequestParam Long categoryId,
		@RequestParam String productName, @RequestParam String brand, @RequestParam String modelName,
		@RequestParam long price, @RequestParam int stockQuantity,
		@RequestParam(defaultValue = "") String productDescription,
		@RequestParam(defaultValue = "") String imageUrl,
		@RequestParam(required = false) MultipartFile imageFile, @RequestParam String salesStatus,
		@RequestParam(name = "specKey", required = false) List<String> specKeys,
		@RequestParam(name = "specValue", required = false) List<String> specValues,
		RedirectAttributes redirect) {
		if (price < 1_000 || price % 100 != 0) {
			redirect.addFlashAttribute("error", "가격은 1,000원 이상부터 100원 단위로 입력해 주세요.");
			return "redirect:/admin/products";
		}
		CategoryEntity category = categories.findById(categoryId).orElseThrow(() -> notFound("카테고리"));
		ProductEntity product = id == null ? new ProductEntity(category, productName, brand, modelName) : product(id);
		product.updateAdmin(category, productName, brand, modelName, price, Math.max(0, stockQuantity),
			productDescription, saveProductImage(imageFile, imageUrl), salesStatus, createSpecJson(specKeys, specValues));
		products.save(product);
		redirect.addFlashAttribute("message", "상품 정보를 저장했습니다.");
		return "redirect:/admin/products";
	}

	@GetMapping("/admin/presets")
	public String presetList(@RequestParam(defaultValue = "") String purpose, Model model) {
		List<QuoteEntity> presets = quotes.findByQuoteTypeOrderByQuoteIdAsc(QuoteType.PRESET).stream()
			.filter(q -> purpose.isBlank() || (q.getPurposeTag() != null && q.getPurposeTag().name().equals(purpose))).toList();
		model.addAttribute("presets", presets);
		model.addAttribute("purpose", purpose);
		return "admin/preset-list";
	}

	@GetMapping({"/admin/presets/new", "/admin/presets/{id}/edit"})
	public String presetForm(@PathVariable(required = false) Long id, Model model) {
		QuoteEntity preset = id == null ? null : quotes.findByQuoteIdAndQuoteType(id, QuoteType.PRESET)
			.orElseThrow(() -> notFound("프리셋"));
		List<ProductEntity> allProducts = products.findAll();
		Map<Long, ProductEntity> productsById = allProducts.stream()
			.collect(Collectors.toMap(ProductEntity::getProductId, Function.identity()));
		Map<Long, Integer> selected = preset == null ? Map.of() : preset.getItems().stream()
			.collect(Collectors.toMap(i -> i.getProductId(), i -> i.getQuantity()));
		Map<String, Integer> selectedQuantity = preset == null ? Map.of() : preset.getItems().stream()
			.collect(Collectors.toMap(
				i -> productsById.get(i.getProductId()).getCategory().getCategoryName(),
				i -> i.getQuantity()
			));
		model.addAttribute("preset", preset);
		model.addAttribute("selected", selected);
		model.addAttribute("selectedQuantity", selectedQuantity);
		model.addAttribute("categories", categories.findAll());
		model.addAttribute("products", allProducts);
		return "admin/preset-form";
	}

	@PostMapping("/admin/presets/save")
	@Transactional
	public String savePreset(@RequestParam(required = false) Long id, @RequestParam String quoteName,
		@RequestParam PurposeTag purposeTag, @RequestParam(defaultValue = "") String summaryDescription,
		@RequestParam(defaultValue = "") String imageUrl,
		@RequestParam(required = false) MultipartFile imageFile,
		@RequestParam(name = "productId") List<Long> productIds,
		@RequestParam(name = "quantity") List<Integer> quantities, Principal principal, RedirectAttributes redirect) {
		List<PresetUpsertRequest.PresetItem> items = new ArrayList<>();
		for (int i = 0; i < Math.min(productIds.size(), quantities.size()); i++) {
			if (productIds.get(i) != null && quantities.get(i) != null && quantities.get(i) > 0) {
				items.add(new PresetUpsertRequest.PresetItem(productIds.get(i), quantities.get(i)));
			}
		}
		PresetUpsertRequest request = new PresetUpsertRequest(quoteName, purposeTag, summaryDescription,
			saveImage(imageFile, imageUrl, "presets"), items);
		Long savedId = id == null
			? presetAdminService.createPreset(request, members.findByLoginId(principal.getName()).orElseThrow(() -> notFound("관리자")).getId())
			: id;
		if (id != null) presetAdminService.updatePreset(id, request);
		redirect.addFlashAttribute("message", "추천 프리셋을 저장했습니다.");
		return id == null ? "redirect:/admin/presets/" + savedId + "/edit" : "redirect:/admin/presets";
	}

	@PostMapping("/admin/presets/{id}/delete")
	@Transactional
	public String deletePreset(@PathVariable Long id, RedirectAttributes redirect) {
		presetAdminService.deletePreset(id);
		redirect.addFlashAttribute("message", "추천 프리셋을 삭제했습니다.");
		return "redirect:/admin/presets";
	}

	@GetMapping("/admin/orders")
	public String orderList(@RequestParam(defaultValue = "") String keyword,
		@RequestParam(defaultValue = "") String status,
		@RequestParam(defaultValue = "false") boolean returnRequested,
		@RequestParam(defaultValue = "1") int page, Model model) {
		String needle = keyword.toLowerCase(Locale.ROOT).trim();
		List<AdminOrder> result = enrichOrders(orders.findAllByOrderByOrderedAtDesc()).stream()
			.filter(o -> status.isBlank() || o.order().getOrderStatus().name().equals(status))
			.filter(o -> !returnRequested || o.order().isReturnRequested())
			.filter(o -> needle.isBlank() || (o.order().getOrderNumber() + " " + o.memberName()).toLowerCase(Locale.ROOT).contains(needle)).toList();
		addPage(model, "orders", result, page);
		model.addAttribute("keyword", keyword);
		model.addAttribute("status", status);
		model.addAttribute("returnRequested", returnRequested);
		return "admin/order-list";
	}

	@GetMapping("/admin/orders/{orderNumber}")
	public String orderDetail(@PathVariable String orderNumber, Model model) {
		OrderEntity order = orders.findByOrderNumber(orderNumber).orElseThrow(() -> notFound("주문"));
		order.getGroups().forEach(group -> group.getItems().size());
		model.addAttribute("order", order);
		model.addAttribute("member", member(order.getMemberId()));
		model.addAttribute("payment", payments.findByOrderId(order.getOrderId()).orElse(null));
		return "admin/order-detail";
	}

	@PostMapping("/admin/orders/{orderNumber}/status")
	@Transactional
	public String updateOrderStatus(@PathVariable String orderNumber, @RequestParam OrderStatus status,
		RedirectAttributes redirect) {
		orders.findByOrderNumber(orderNumber).orElseThrow(() -> notFound("주문")).changeStatus(status);
		redirect.addFlashAttribute("message", "주문 상태를 변경했습니다.");
		return "redirect:/admin/orders/" + orderNumber;
	}

	@GetMapping("/admin/ai-logs")
	public String aiLogs(Model model) {
		List<AiRecommendationEntity> recommendations = aiRecommendations.findAllByOrderByAiRecommendationIdDesc();
		Map<Long, Member> membersById = members.findAllById(recommendations.stream()
			.map(ai -> ai.getQuote().getMemberId()).distinct().toList()).stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));
		List<AiLog> logs = recommendations.stream()
			.map(ai -> new AiLog(ai, membersById.get(ai.getQuote().getMemberId()),
				formatJson(ai.getAiAnswerJson())))
			.toList();
		model.addAttribute("logs", logs);
		return "admin/ai-log-list";
	}

	private Member member(Long id) { return members.findById(id).orElseThrow(() -> notFound("회원")); }
	private ProductEntity product(Long id) { return products.findById(id).orElseThrow(() -> notFound("상품")); }
	private <T> void addPage(Model model, String attribute, List<T> result, int requestedPage) {
		int totalPages = Math.max(1, (result.size() + ADMIN_PAGE_SIZE - 1) / ADMIN_PAGE_SIZE);
		int currentPage = Math.min(Math.max(1, requestedPage), totalPages);
		int fromIndex = Math.min((currentPage - 1) * ADMIN_PAGE_SIZE, result.size());
		int toIndex = Math.min(fromIndex + ADMIN_PAGE_SIZE, result.size());
		int firstPage = Math.max(1, currentPage - 2);
		int lastPage = Math.min(totalPages, firstPage + 4);
		firstPage = Math.max(1, lastPage - 4);
		List<Integer> pageNumbers = new ArrayList<>();
		for (int number = firstPage; number <= lastPage; number++) pageNumbers.add(number);

		model.addAttribute(attribute, result.subList(fromIndex, toIndex));
		model.addAttribute("currentPage", currentPage);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("totalElements", result.size());
		model.addAttribute("pageNumbers", pageNumbers);
	}
	private String formatJson(String value) {
		if (value == null || value.isBlank()) return value;
		try {
			return objectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(objectMapper.readTree(value));
		} catch (IOException exception) {
			return value;
		}
	}
	private String createSpecJson(List<String> keys, List<String> values) {
		Map<String, String> specs = new java.util.LinkedHashMap<>();
		if (keys != null && values != null) {
			for (int i = 0; i < Math.min(keys.size(), values.size()); i++) {
				String value = values.get(i) == null ? "" : values.get(i).trim();
				if (!value.isBlank()) specs.put(keys.get(i), value);
			}
		}
		try {
			return objectMapper.writeValueAsString(specs);
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상세 스펙을 저장할 수 없습니다.", exception);
		}
	}
	private String saveProductImage(MultipartFile file, String currentUrl) {
		return saveImage(file, currentUrl, "products");
	}
	private String saveImage(MultipartFile file, String currentUrl, String folder) {
		if (file == null || file.isEmpty()) return currentUrl;
		String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
		String extension = original.lastIndexOf('.') < 0 ? "" : original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
		if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(extension)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPG, PNG, WEBP, GIF 이미지만 등록할 수 있습니다.");
		}
		try {
			Path directory = Path.of("uploads", folder).toAbsolutePath();
			Files.createDirectories(directory);
			String filename = UUID.randomUUID() + extension;
			file.transferTo(directory.resolve(filename));
			return "/uploads/" + folder + "/" + filename;
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "상품 이미지를 저장하지 못했습니다.", exception);
		}
	}
	private ResponseStatusException notFound(String name) { return new ResponseStatusException(HttpStatus.NOT_FOUND, name + "을(를) 찾을 수 없습니다."); }
	private List<AdminOrder> enrichOrders(List<OrderEntity> list) {
		Map<Long, Member> map = members.findAllById(list.stream().map(OrderEntity::getMemberId).distinct().toList())
			.stream().collect(Collectors.toMap(Member::getId, Function.identity()));
		return list.stream().map(o -> new AdminOrder(o, map.get(o.getMemberId()) == null ? "탈퇴 회원" : map.get(o.getMemberId()).getName())).toList();
	}

	public record AdminOrder(OrderEntity order, String memberName) {}
	public record AiLog(AiRecommendationEntity recommendation, Member member, String formattedAnswer) {}
}

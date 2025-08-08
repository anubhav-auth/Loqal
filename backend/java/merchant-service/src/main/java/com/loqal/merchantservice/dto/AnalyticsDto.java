import com.loqal.merchantservice.dto.ProductDto;

import java.util.List;

public record AnalyticsDto(double totalRevenue, int ordersToday, List<ProductDto> topSellingProducts) {}
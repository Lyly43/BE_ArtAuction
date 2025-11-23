package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.BaseSearchRequest;
import com.auctionaa.backend.DTO.Request.CreateInvoiceRequest;
import com.auctionaa.backend.DTO.Response.InvoiceListItemDTO;
import com.auctionaa.backend.DTO.Response.InvoiceSmall;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.InvoiceRepository;
import com.auctionaa.backend.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ArtworkRepository artworkRepository;
    private final AuctionRoomRepository auctionRoomRepository;
    private final GenericSearchService genericSearchService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          UserRepository userRepository,
                          ArtworkRepository artworkRepository,
                          AuctionRoomRepository auctionRoomRepository,
                          GenericSearchService genericSearchService) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.artworkRepository = artworkRepository;
        this.auctionRoomRepository = auctionRoomRepository;
        this.genericSearchService = genericSearchService;
    }

    public Page<Invoice> getAllInvoice(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    public Invoice createInvoice(CreateInvoiceRequest req) {
        // Validate tồn tại
        User buyer = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Artwork art = artworkRepository.findById(req.getArtworkId())
                .orElseThrow(() -> new RuntimeException("Artwork not found"));
        AuctionRoom room = auctionRoomRepository.findById(req.getAuctionRoomId())
                .orElseThrow(() -> new RuntimeException("Auction room not found"));

        // Snapshot + set IDs
        Invoice iv = new Invoice();
        iv.generateId();

        iv.setUserId(buyer.getId());
        iv.setArtworkId(art.getId());
        iv.setAuctionRoomId(room.getId());

        iv.setArtworkTitle(art.getTitle());
        /* iv.setArtistName(art.getArtistName()); */// <-- dùng đúng tên nghệ sĩ
        iv.setRoomName(room.getRoomName());

        // Giao dịch
        iv.setAmount(req.getAmount());
        iv.setPaymentMethod(req.getPaymentMethod());
        iv.setPaymentStatus(req.getPaymentStatus());
        iv.setTransactionType(req.getTransactionType());
        iv.setShippingFee(req.getShippingFee());
        iv.setTotalAmount(req.getTotalAmount());
        iv.setPaymentDate(req.getPaymentDate());
        iv.setOrderDate(req.getOrderDate());

        iv.setRecipientNameText(req.getRecipientNameText());
        iv.setNote(req.getNote());
        iv.setCreatedAt(LocalDateTime.now());
        return invoiceRepository.save(iv);
    }

    // Trả Page<Invoice> cho trang admin/list
    public Page<Invoice> getByOwnerEmail(String email, Pageable pageable) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return invoiceRepository.findByUserId(user.getId(), pageable);
    }

    public List<InvoiceListItemDTO> getMyInvoicesArray(String email) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var invoices = invoiceRepository.findByUserIdOrderByOrderDateDesc(user.getId());

        // Lấy tất cả artworkId trong list invoice
        var artworkIds = invoices.stream()
                .map(Invoice::getArtworkId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        // Tải batch artworks để tránh N+1
        var artworkMap = artworkRepository.findAllById(artworkIds).stream()
                .collect(java.util.stream.Collectors.toMap(Artwork::getId, a -> a));

        // Map về DTO
        return invoices.stream()
                .map(iv -> {
                    Artwork art = artworkMap.get(iv.getArtworkId());
                    String avt = (art != null) ? art.getAvtArtwork() : null;
                    String title = (art != null && art.getTitle() != null) ? art.getTitle()
                            : iv.getArtworkTitle(); // ưu tiên title hiện tại

                    return new InvoiceListItemDTO(
                            iv.getId(),
                            iv.getAuctionRoomId(),
                            iv.getArtworkId(), // ✅ trả id artwork
                            title, // ✅ trả title để FE show
                            avt, // ✅ trả ảnh avtArtwork
                            iv.getTotalAmount(),
                            iv.getPaymentStatus(),
                            iv.getCreatedAt());
                })
                .toList();
    }

    /**
     * Tìm kiếm và lọc invoice của user hiện tại
     * - Tìm theo ID
     * - Tìm theo artworkTitle (tên artwork) hoặc roomName (tên phòng)
     * - Lọc theo createdAt hoặc paymentDate (ngày)
     * - Filter theo userId (chỉ lấy invoice của user đó)
     * Note: Invoice không có field "type" nên sẽ bỏ qua type filter
     */
    public List<Invoice> searchAndFilter(BaseSearchRequest request, String userId) {
        // Invoice có thể tìm theo artworkTitle hoặc roomName
        // Nếu có name, tìm trong cả 2 field
        if (request.getName() != null && !request.getName().isEmpty()) {
            // Sử dụng generic search với nameField = artworkTitle
            // Sau đó filter thêm theo roomName nếu cần
            List<Invoice> results = genericSearchService.searchAndFilter(
                    request,
                    Invoice.class,
                    "_id", // idField
                    "artworkTitle", // nameField (tìm theo tên artwork)
                    null, // typeField (không có)
                    request.getDateFrom() != null || request.getDateTo() != null
                            ? "createdAt"
                            : null, // dateField
                    "userId", // userIdField
                    userId // userId
            );

            // Nếu có name và không tìm thấy trong artworkTitle, thử tìm trong roomName
            if (results.isEmpty() && request.getName() != null) {
                BaseSearchRequest roomNameRequest = new BaseSearchRequest();
                roomNameRequest.setId(request.getId());
                roomNameRequest.setName(request.getName());
                roomNameRequest.setDateFrom(request.getDateFrom());
                roomNameRequest.setDateTo(request.getDateTo());

                return genericSearchService.searchAndFilter(
                        roomNameRequest,
                        Invoice.class,
                        "_id",
                        "roomName", // tìm theo tên phòng
                        null,
                        roomNameRequest.getDateFrom() != null
                                || roomNameRequest.getDateTo() != null
                                ? "createdAt"
                                : null,
                        "userId", // userIdField
                        userId // userId
                );
            }

            return results;
        }

        // Nếu không có name, chỉ tìm theo ID và date
        return genericSearchService.searchAndFilter(
                request,
                Invoice.class,
                "_id",
                null, // không tìm theo name
                null, // không có type
                request.getDateFrom() != null || request.getDateTo() != null
                        ? "createdAt"
                        : null,
                "userId", // userIdField
                userId // userId
        );
    }

    public InvoiceSmall getLatestInvoiceSmallForUser(String userId) {
        Invoice invoice = invoiceRepository
                .findTopByUserIdOrderByOrderDateDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn nào cho user"
                ));

        String totalAmountStr = invoice.getTotalAmount() != null
                ? invoice.getTotalAmount().toPlainString()
                : null;

        return new InvoiceSmall(
                invoice.getArtworkTitle(),
                invoice.getArtworkImageUrl(),
                invoice.getRoomName(),
                totalAmountStr,
                invoice.getPaymentStatus()  // int
        );
    }

}

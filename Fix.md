# API Nam Fix User

## 1. API xác thực KYC (CCCD + Selfie) → role = 1, kycStatus = 1

### a. Test post man
> Post http://localhost:8081/api/user/kyc/verify

>Headers:
Authorization: Bearer YOUR_JWT_TOKEN

> Body (form-data):
> 
> - cccdFront
> 
> - cccdBack
> 
> - selfie
### b, kết quả 
>"kycId": "691cced6a45c69539c72cf81",
>"kycStatus": 1,
>"role": 1,
"faceMatchScore": 98.026,
"livenessScore": 1.0,
"message": "Xác thực KYC thành công, bạn đã được nâng lên Buyer"


## 2. API lấy thông tin user (trả về kycStatus và role)

### a. Test post man
> Method: GET
>- URL: http://localhost:8081/api/user/info
>- Headers:
>- Authorization: Bearer YOUR_JWT_TOKEN

### b, kết quả
>{
> - "id": "U-531707855758300",
> - "username": "Nguyen Ly",
> - "email": "vodangnam30102004@gmail.com",
> - "phonenumber": null,
> - "status": 1,
> - "cccd": "048204003042",
   >- "address": "Tổ 8\nHòa Hải, Ngũ Hành Sơn, Đà Nẵng",
> - "avt": null,
> -"createdAt": "2025-11-12T20:21:19.532",
> -"updatedAt": "2025-11-19T03:04:29.512",
> -"dateOfBirth": "2004-06-14",
> -"gender": 0,
> -"role": 1,
> -"kycStatus": 1
}
> - 
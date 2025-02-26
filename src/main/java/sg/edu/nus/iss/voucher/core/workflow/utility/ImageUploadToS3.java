package sg.edu.nus.iss.voucher.core.workflow.utility;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;

import sg.edu.nus.iss.voucher.core.workflow.configuration.AWSConfig;

@Component
public class ImageUploadToS3 {
	
	private static final Logger logger = LoggerFactory.getLogger(ImageUploadToS3.class);

	public static boolean checkImageExistBeforeUpload(AmazonS3 s3Client, MultipartFile multipartFile,
			AWSConfig awsConfig, String keyPrefix) {
		try {
			boolean isImageExists = s3Client.doesObjectExist(awsConfig.getS3Bucket(),
					keyPrefix.trim() + multipartFile.getOriginalFilename().trim());

			logger.info("Image already uploaded to s3. " + isImageExists);

			if (!isImageExists) {
				boolean isUploaded  = false;
				
				 isUploaded = ImageUploadToS3.imageUpload(s3Client, multipartFile, awsConfig, keyPrefix);
				
				if (isUploaded) {
					return true;
				}
			} else {
				return true;
			}
		} catch (Exception e) {
			logger.error("Error while checkImageExistBeforeUpload : " + e.getMessage());

		}
		return false;
	}
	

	public static boolean imageUpload(AmazonS3 s3Client, MultipartFile multipartFile,
			AWSConfig awsConfig, String keyPrefix) {

		String bucketName = awsConfig.getS3Bucket().trim();
		String uploadFileName = multipartFile.getOriginalFilename();

		try {
			if (!multipartFile.isEmpty()) {

				InputStream is = multipartFile.getInputStream();
				PutObjectResult putObjResult = s3Client.putObject(bucketName, keyPrefix + uploadFileName, is, null);
				logger.info("Object ETag:" + putObjResult.getETag());
				return true;
			}
		} catch (Exception e) {
			logger.error("Error while uploading image: " + e.getMessage());
			return false;
		}
		return false;
	}
}

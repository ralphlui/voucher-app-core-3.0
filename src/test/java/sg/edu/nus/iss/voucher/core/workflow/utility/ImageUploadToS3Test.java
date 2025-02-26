package sg.edu.nus.iss.voucher.core.workflow.utility;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;

import sg.edu.nus.iss.voucher.core.workflow.configuration.AWSConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class ImageUploadToS3Test {

	@Mock
	private AmazonS3 s3Client;

	@Mock
	private MultipartFile multipartFile;

	@Mock
	private AWSConfig awsConfig;

	@Mock
	private GeneratePresignedUrlRequest generatePresignedUrlRequest;

	@InjectMocks
	private ImageUploadToS3 imageUploadToS3;
	
	private String bucketName = "";
	private String keyPrefix = "";
	private String fileName = "";
	private String imageKey = "";

	
	@BeforeEach
	void setUp() {
		 bucketName = "voucher-app";
		 keyPrefix = "images/";
		 fileName = "test.jpg";
		 imageKey = "images/test.jpg";
	}

	@Test
	public void testImageUpload() throws IOException {


		Mockito.when(awsConfig.getS3Bucket()).thenReturn(bucketName);

		Mockito.when(multipartFile.isEmpty()).thenReturn(false);
		Mockito.when(multipartFile.getOriginalFilename()).thenReturn(fileName);

		Mockito.when(s3Client.putObject(any(), any(), any(), any())).thenReturn(mock(PutObjectResult.class));

		boolean result = ImageUploadToS3.imageUpload(s3Client, multipartFile, awsConfig, keyPrefix);

		assertTrue(result);

	}

	@Test
	public void testCheckImageExistBeforeUpload_ImageExists() {


		MultipartFile multipartFile = mock(MultipartFile.class);
		String keyPrefix = "images/";

		Mockito.when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
		Mockito.when(awsConfig.getS3Bucket()).thenReturn(bucketName);
		Mockito.when(s3Client.doesObjectExist(bucketName, imageKey)).thenReturn(true);

		boolean result = ImageUploadToS3.checkImageExistBeforeUpload(s3Client, multipartFile, awsConfig,
				keyPrefix);

		assertTrue(result);
	}

	@Test
	public void testCheckImageExistBeforeUpload_ImageDoesNotExist() {
		
		MultipartFile multipartFile = mock(MultipartFile.class);
		String keyPrefix = "images/";

		Mockito.when(multipartFile.getOriginalFilename()).thenReturn("image.jpg");
		Mockito.when(awsConfig.getS3Bucket()).thenReturn(bucketName);
		Mockito.when(s3Client.doesObjectExist(bucketName, imageKey)).thenReturn(false);
		Mockito.when(s3Client.putObject(any(), any(), any(), any())).thenReturn(mock(PutObjectResult.class));

		boolean result = ImageUploadToS3.checkImageExistBeforeUpload(s3Client, multipartFile, awsConfig,
				keyPrefix);

		assertTrue(result);
	}
	
}

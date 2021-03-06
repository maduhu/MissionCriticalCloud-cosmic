package com.cloud.storage.datastore.driver;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.api.ApiConstants;
import com.cloud.configuration.Config;
import com.cloud.engine.subsystem.api.storage.DataObject;
import com.cloud.engine.subsystem.api.storage.DataStore;
import com.cloud.framework.config.dao.ConfigurationDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.datastore.db.ImageStoreDetailsDao;
import com.cloud.storage.image.BaseImageStoreDriverImpl;
import com.cloud.storage.image.store.ImageStoreImpl;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.storage.S3.S3Utils;

import javax.inject.Inject;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3ImageStoreDriverImpl extends BaseImageStoreDriverImpl {
    private static final Logger s_logger = LoggerFactory.getLogger(S3ImageStoreDriverImpl.class);

    @Inject
    ImageStoreDetailsDao _imageStoreDetailsDao;

    @Inject
    ConfigurationDao _configDao;

    @Override
    public String createEntityExtractUrl(final DataStore store, final String key, final ImageFormat format, final DataObject dataObject) {
        /**
         * Generate a pre-signed URL for the given object.
         */
        final S3TO s3 = (S3TO) getStoreTO(store);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Generating pre-signed s3 entity extraction URL for object: " + key);
        }
        final Date expiration = new Date();
        long milliSeconds = expiration.getTime();

        // Get extract url expiration interval set in global configuration (in seconds)
        final String urlExpirationInterval = _configDao.getValue(Config.ExtractURLExpirationInterval.toString());

        // Expired after configured interval (in milliseconds), default 14400 seconds
        milliSeconds += 1000 * NumbersUtil.parseInt(urlExpirationInterval, 14400);
        expiration.setTime(milliSeconds);

        final URL s3url = S3Utils.generatePresignedUrl(s3, s3.getBucketName(), key, expiration);

        s_logger.info("Pre-Signed URL = " + s3url.toString());

        return s3url.toString();
    }

    @Override
    public DataStoreTO getStoreTO(final DataStore store) {
        final ImageStoreImpl imgStore = (ImageStoreImpl) store;
        final Map<String, String> details = _imageStoreDetailsDao.getDetails(imgStore.getId());
        return new S3TO(imgStore.getId(),
                imgStore.getUuid(),
                details.get(ApiConstants.S3_ACCESS_KEY),
                details.get(ApiConstants.S3_SECRET_KEY),
                details.get(ApiConstants.S3_END_POINT),
                details.get(ApiConstants.S3_BUCKET_NAME),
                details.get(ApiConstants.S3_SIGNER),
                details.get(ApiConstants.S3_HTTPS_FLAG) == null ? false : Boolean.parseBoolean(details.get(ApiConstants.S3_HTTPS_FLAG)),
                details.get(ApiConstants.S3_CONNECTION_TIMEOUT) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_CONNECTION_TIMEOUT)),
                details.get(ApiConstants.S3_MAX_ERROR_RETRY) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_MAX_ERROR_RETRY)),
                details.get(ApiConstants.S3_SOCKET_TIMEOUT) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_SOCKET_TIMEOUT)),
                imgStore.getCreated(),
                _configDao.getValue(Config.S3EnableRRS.toString()) == null ? false : Boolean.parseBoolean(_configDao.getValue(Config.S3EnableRRS.toString())),
                getMaxSingleUploadSizeInBytes(),
                details.get(ApiConstants.S3_CONNECTION_TTL) == null ? null : Integer.valueOf(details.get(ApiConstants.S3_CONNECTION_TTL)),
                details.get(ApiConstants.S3_USE_TCP_KEEPALIVE) == null ? null : Boolean.parseBoolean(details.get(ApiConstants.S3_USE_TCP_KEEPALIVE)));
    }

    private long getMaxSingleUploadSizeInBytes() {
        try {
            return Long.parseLong(_configDao.getValue(Config.S3MaxSingleUploadSize.toString())) * 1024L * 1024L * 1024L;
        } catch (final NumberFormatException e) {
            // use default 1TB
            return 1024L * 1024L * 1024L * 1024L;
        }
    }
}

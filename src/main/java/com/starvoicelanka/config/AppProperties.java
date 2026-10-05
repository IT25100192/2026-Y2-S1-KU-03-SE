package com.starvoicelanka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "starvoice")
public class AppProperties {
    private int freeVotesPerRound = 5;
    private double votePriceLkr = 25.0;
    private String notifyProvider = "console";
    private String sendgridApiKey = "";
    private String sendgridFrom = "no-reply@starvoice.lk";
    private String twilioAccountSid = "";
    private String twilioAuthToken = "";
    private String twilioFromNumber = "";
    private String defaultCountryCode = "+94";
    private int notifyMaxAttempts = 3;
    private String paymentWebhookSecret = "starvoice-dev-webhook-secret";
    private long mediaMaxBytes = 25 * 1024 * 1024L; // 25MB
    private String mediaDir = "uploads/performances";
    private int fraudSharedIpVoters = 3;
    private int fraudBurstPerMinute = 5;
    private int fraudFreshAccountSeconds = 60;
    private int voteRateLimitPerMinute = 20;
    private boolean workersEnabled = true;

    // Getters and Setters
    public int getFreeVotesPerRound() { return freeVotesPerRound; }
    public void setFreeVotesPerRound(int freeVotesPerRound) { this.freeVotesPerRound = freeVotesPerRound; }

    public double getVotePriceLkr() { return votePriceLkr; }
    public void setVotePriceLkr(double votePriceLkr) { this.votePriceLkr = votePriceLkr; }
    public double getVotePriceLKR() { return votePriceLkr; }

    public String getNotifyProvider() { return notifyProvider; }
    public void setNotifyProvider(String notifyProvider) { this.notifyProvider = notifyProvider; }

    public String getSendgridApiKey() { return sendgridApiKey; }
    public void setSendgridApiKey(String sendgridApiKey) { this.sendgridApiKey = sendgridApiKey; }

    public String getSendgridFrom() { return sendgridFrom; }
    public void setSendgridFrom(String sendgridFrom) { this.sendgridFrom = sendgridFrom; }

    public String getTwilioAccountSid() { return twilioAccountSid; }
    public void setTwilioAccountSid(String twilioAccountSid) { this.twilioAccountSid = twilioAccountSid; }

    public String getTwilioAuthToken() { return twilioAuthToken; }
    public void setTwilioAuthToken(String twilioAuthToken) { this.twilioAuthToken = twilioAuthToken; }

    public String getTwilioFromNumber() { return twilioFromNumber; }
    public void setTwilioFromNumber(String twilioFromNumber) { this.twilioFromNumber = twilioFromNumber; }

    public String getDefaultCountryCode() { return defaultCountryCode; }
    public void setDefaultCountryCode(String defaultCountryCode) { this.defaultCountryCode = defaultCountryCode; }

    public int getNotifyMaxAttempts() { return notifyMaxAttempts; }
    public void setNotifyMaxAttempts(int notifyMaxAttempts) { this.notifyMaxAttempts = notifyMaxAttempts; }

    public String getPaymentWebhookSecret() { return paymentWebhookSecret; }
    public void setPaymentWebhookSecret(String paymentWebhookSecret) { this.paymentWebhookSecret = paymentWebhookSecret; }

    public long getMediaMaxBytes() { return mediaMaxBytes; }
    public void setMediaMaxBytes(long mediaMaxBytes) { this.mediaMaxBytes = mediaMaxBytes; }

    public String getMediaDir() { return mediaDir; }
    public void setMediaDir(String mediaDir) { this.mediaDir = mediaDir; }
    public String getUploadDir() { return mediaDir; }

    public int getFraudSharedIpVoters() { return fraudSharedIpVoters; }
    public void setFraudSharedIpVoters(int fraudSharedIpVoters) { this.fraudSharedIpVoters = fraudSharedIpVoters; }

    public int getFraudBurstPerMinute() { return fraudBurstPerMinute; }
    public void setFraudBurstPerMinute(int fraudBurstPerMinute) { this.fraudBurstPerMinute = fraudBurstPerMinute; }

    public int getFraudFreshAccountSeconds() { return fraudFreshAccountSeconds; }
    public void setFraudFreshAccountSeconds(int fraudFreshAccountSeconds) { this.fraudFreshAccountSeconds = fraudFreshAccountSeconds; }

    public int getVoteRateLimitPerMinute() { return voteRateLimitPerMinute; }
    public void setVoteRateLimitPerMinute(int voteRateLimitPerMinute) { this.voteRateLimitPerMinute = voteRateLimitPerMinute; }

    public boolean isWorkersEnabled() { return workersEnabled; }
    public void setWorkersEnabled(boolean workersEnabled) { this.workersEnabled = workersEnabled; }
}

package com.aerionsoft.notification.dispatcher;

import com.aerionsoft.notification.channel.NotificationChannel;
import com.aerionsoft.notification.dispatcher.recipient.NotificationRecipientResolver;
import com.aerionsoft.notification.entity.Notification;
import com.aerionsoft.notification.entity.NotificationDelivery;
import com.aerionsoft.notification.enums.NotificationChannelType;
import com.aerionsoft.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DefaultNotificationDispatcher implements NotificationDispatcher {

    private final NotificationRoutingResolver routingResolver;
    private final Map<NotificationChannelType, NotificationChannel> channelsByType;
    private final Map<NotificationChannelType, NotificationRecipientResolver> recipientResolversByType;
    private final NotificationRepository notificationRepository;
    private final Executor notificationExecutor;

    public DefaultNotificationDispatcher(NotificationRoutingResolver routingResolver,
                                         List<NotificationChannel> channels,
                                         List<NotificationRecipientResolver> recipientResolvers,
                                         NotificationRepository notificationRepository,
                                         Executor notificationExecutor) {
        this.routingResolver = routingResolver;
        this.channelsByType = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getType, c -> c));
        this.recipientResolversByType = recipientResolvers.stream()
                .collect(Collectors.toMap(NotificationRecipientResolver::getChannelType, r -> r));
        this.notificationRepository = notificationRepository;
        this.notificationExecutor = notificationExecutor;
    }

    @Override
    public void dispatch(Notification notification, Map<NotificationChannelType, String> recipientContacts) {
        Set<NotificationChannelType> resolvedChannels = routingResolver.resolve(notification);

        if (resolvedChannels.isEmpty()) {
            log.debug("No channels resolved for notification id={}, userId={}",
                    notification.getId(), notification.getUserId());
            return;
        }

        List<CompletableFuture<Void>> futures = resolvedChannels.stream()
                .map(channelType -> dispatchToChannel(notification, channelType, recipientContacts))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .exceptionally(ex -> {
                    log.error("One or more channel dispatches failed for notification id={}",
                            notification.getId(), ex);
                    return null;
                });
    }

    private CompletableFuture<Void> dispatchToChannel(Notification notification,
                                                      NotificationChannelType channelType,
                                                      Map<NotificationChannelType, String> recipientContacts) {
        NotificationChannel channel = channelsByType.get(channelType);
        if (channel == null) {
            log.warn("No channel implementation registered for type={}", channelType);
            return CompletableFuture.completedFuture(null);
        }

        String recipient = resolveRecipient(notification, channelType, recipientContacts);

        NotificationDelivery delivery = NotificationDelivery.pendingFor(channelType);
        delivery.setRecipient(recipient);
        notification.addDelivery(delivery);

        return CompletableFuture.runAsync(() -> {
            channel.send(notification, delivery);
            notificationRepository.save(notification);
        }, notificationExecutor).exceptionally(ex -> {
            log.error("Unhandled exception dispatching notification id={} via channel={}",
                    notification.getId(), channelType, ex);
            delivery.markFailed(ex.getMessage());
            notificationRepository.save(notification);
            return null;
        });
    }

    private String resolveRecipient(Notification notification,
                                    NotificationChannelType channelType,
                                    Map<NotificationChannelType, String> recipientContacts) {
        NotificationRecipientResolver resolver = recipientResolversByType.get(channelType);
        if (resolver == null) {
            log.warn("No recipient resolver registered for channel={}", channelType);
            return null;
        }
        return resolver.resolveRecipient(notification, recipientContacts);
    }
}
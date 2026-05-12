package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.query.application.usecase.IndexConsignmentUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.UpdateConsignmentReturnUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.UpdateConsignmentStatusUseCase;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentClosed;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentIssued;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemsReturned;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ConsignmentIndexListener {

    private final IndexConsignmentUseCase indexConsignment;
    private final UpdateConsignmentReturnUseCase updateConsignmentReturn;
    private final UpdateConsignmentStatusUseCase updateConsignmentStatus;

    public ConsignmentIndexListener(IndexConsignmentUseCase indexConsignment,
                                    UpdateConsignmentReturnUseCase updateConsignmentReturn,
                                    UpdateConsignmentStatusUseCase updateConsignmentStatus) {
        this.indexConsignment       = indexConsignment;
        this.updateConsignmentReturn = updateConsignmentReturn;
        this.updateConsignmentStatus = updateConsignmentStatus;
    }

    @ApplicationModuleListener
    public void on(ConsignmentIssued event) {
        indexConsignment.execute(event);
    }

    @ApplicationModuleListener
    public void on(ConsignmentItemsReturned event) {
        updateConsignmentReturn.execute(event);
    }

    @ApplicationModuleListener
    public void on(ConsignmentClosed event) {
        updateConsignmentStatus.execute(event);
    }
}

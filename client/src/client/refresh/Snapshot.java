package client.refresh;

import dto.EventDTO;
import dto.EventDetailsDTO;
import dto.UserDetailsDTO;
import dto.UserEventDetailsDTO;
import dto.UserSummaryDTO;

import java.util.List;

/**
 * Everything the screens show, as pulled from the server in one refresh round.
 * eventDetails / userEventDetails are null when no event is selected.
 */
public record Snapshot(
        List<EventDTO> events,
        List<UserSummaryDTO> users,
        UserDetailsDTO me,
        EventDetailsDTO eventDetails,
        UserEventDetailsDTO userEventDetails
) {}

package org.talend.dataprep.upgrades;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.service.info.VersionService;
import org.talend.dataprep.info.Version;

@Component
public class Upgrades {

    @Autowired
    Optional<VersionService> versionService;

    @Autowired(required = false)
    List<UpgradeTask> tasks = new ArrayList<>();

    public List<UpgradeTask> getTasks(Version to) {
        final Version from = new Version();
        from.setVersionId("0.0.0");
        from.setBuildId("0");
        return getTasks(from, to);
    }

    public List<UpgradeTask> getTasks(Version from, Version to) {
        return tasks.stream() //
                // .filter(t -> t.getVersion().compareTo(to) <= 0 && t.getVersion().compareTo(from) > 0) // TODO Implement comparable
                .collect(Collectors.toList());
    }

    public void register(UpgradeTask task) {
        tasks.add(task);
    }
}

package net.minecraftforge.common.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.IRequirementsStrategy;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public abstract class AdvancementProvider implements IDataProvider
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
	private final DataGenerator gen;
    private final ExistingFileHelper existingFileHelper;

	public AdvancementProvider(DataGenerator gen, ExistingFileHelper existingFileHelper)
    {
        this.gen = gen;
        this.existingFileHelper = existingFileHelper;
    }

    protected abstract void registerAdvancements(BiConsumer<Advancement.Builder, ResourceLocation> consumer);

    @Override
    public void act(DirectoryCache cache) throws IOException
    {
        AdvancementBuilder.existingFileHelper = existingFileHelper;
        Path outputFolder = this.gen.getOutputFolder();
        Set<ResourceLocation> set = new HashSet<>();
        this.registerAdvancements((builder, id) -> 
        {
            if(!set.add(id))
            {
                throw new IllegalStateException("Duplicate advancement " + id);
            } else
            {
                Path path = getPath(outputFolder, id);

                try
                {
                    IDataProvider.save(GSON, cache, builder.serialize(), path);
                } catch (IOException e)
                {
                    LOGGER.error("Couldn't save advancement {}", path, e);
                }
            }
        });
    }

    @Override
    public String getName()
    {
        return "Advancements";
    }

    private static Path getPath(Path pathIn, ResourceLocation location)
    {
        return pathIn.resolve("data/" + location.getNamespace() + "/advancements/" + location.getPath() + ".json");
    }
    
    public static class AdvancementBuilder extends Advancement.Builder
    {
        private ResourceLocation parentId;
        private static ExistingFileHelper existingFileHelper;

        public static AdvancementBuilder builder()
        {
            return new AdvancementBuilder();
        }

        @Override
        public AdvancementBuilder withParent(Advancement parentIn)
        {
            return (AdvancementBuilder) super.withParent(parentIn);
        }
        
        @Override
        public AdvancementBuilder withParentId(ResourceLocation parentIdIn)
        {
            this.parentId = parentIdIn;
            return this;
        }
        
        @Override
        public AdvancementBuilder withDisplay(ItemStack stack, ITextComponent title, ITextComponent description, ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat, boolean hidden)
        {
            return (AdvancementBuilder) super.withDisplay(stack, title, description, background, frame, showToast, announceToChat, hidden);
        }
        
        @Override
        public AdvancementBuilder withDisplay(IItemProvider itemIn, ITextComponent title, ITextComponent description, ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat, boolean hidden)
        {
            return (AdvancementBuilder) super.withDisplay(itemIn, title, description, background, frame, showToast, announceToChat, hidden);
        }
        
        @Override
        public AdvancementBuilder withDisplay(DisplayInfo displayIn)
        {
            return (AdvancementBuilder) super.withDisplay(displayIn);
        }
        
        @Override
        public AdvancementBuilder withRewards(AdvancementRewards.Builder rewardsBuilder)
        {
            return (AdvancementBuilder) super.withRewards(rewardsBuilder);
        }
        
        @Override
        public AdvancementBuilder withRewards(AdvancementRewards rewards)
        {
            return (AdvancementBuilder) super.withRewards(rewards);
        }
        
        @Override
        public AdvancementBuilder withCriterion(String key, ICriterionInstance criterionIn)
        {
            return (AdvancementBuilder) super.withCriterion(key, criterionIn);
        }
        
        @Override
        public AdvancementBuilder withCriterion(String key, Criterion criterionIn)
        {
            return (AdvancementBuilder) super.withCriterion(key, criterionIn);
        }
        
        @Override
        public AdvancementBuilder withRequirementsStrategy(IRequirementsStrategy strategy)
        {
            return (AdvancementBuilder) super.withRequirementsStrategy(strategy);
        }

        @Override
        public boolean resolveParent(Function<ResourceLocation, Advancement> lookup)
        {
            return super.resolveParent(lookup) ? true : existingFileHelper.exists(this.parentId, ResourcePackType.SERVER_DATA, ".json", "advancements");
        }

        public Advancement register(BiConsumer<Advancement.Builder, ResourceLocation> consumer, ResourceLocation id)
        {
            consumer.accept(this, id);
            return this.build(id);
        }
    }
}

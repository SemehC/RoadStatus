package tn.enis.roadstatus.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import tn.enis.roadstatus.db.RoadStatusDatabase
import tn.enis.roadstatus.other.Constants.DATABASE_NAME
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRoadStatusDatabase(
            @ApplicationContext ctx: Context
    )=Room.databaseBuilder(
            ctx,
            RoadStatusDatabase::class.java,
            DATABASE_NAME
    )

    @Singleton
    @Provides
    fun provideRoadStatusDAO(db:RoadStatusDatabase)=db.getRoadStatusDAO()


}